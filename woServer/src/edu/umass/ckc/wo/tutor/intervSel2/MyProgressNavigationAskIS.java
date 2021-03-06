package edu.umass.ckc.wo.tutor.intervSel2;



import ckc.servlet.servbase.ServletParams;
import edu.umass.ckc.wo.content.Problem;
import edu.umass.ckc.wo.event.tutorhut.ContinueNextProblemInterventionEvent;
import edu.umass.ckc.wo.event.tutorhut.InputResponseNextProblemInterventionEvent;
import edu.umass.ckc.wo.event.tutorhut.NextProblemEvent;
import edu.umass.ckc.wo.interventions.*;
import edu.umass.ckc.wo.smgr.SessionManager;
import edu.umass.ckc.wo.tutor.pedModel.PedagogicalModel;
import edu.umass.ckc.wo.tutor.studmod.AffectStudentModel;
import edu.umass.ckc.wo.tutormeta.Intervention;
import edu.umass.ckc.wo.tutormeta.StudentModel;
import edu.umass.ckc.wo.util.State;
import edu.umass.ckc.wo.util.WoProps;
import org.jdom.Element;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: marshall
 * Date: 9/3/14
 * Time: 11:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class MyProgressNavigationAskIS extends NextProblemInterventionSelector {
    MyState state;
    Map<String,Bounds> emotionSettings = new HashMap<String, Bounds>(11);
    boolean checkEmotions=false;
    String emLowerBound=null;
    String emUpperBound=null;
    boolean isAskDialog=true;

    int minIntervalBetweenMPPQueriesBasedOnAffect = 5 * 60 * 1000;  // default: we wait 5 minutes before we ask again about MPP after we show a dialog about it.
                                                                                         // assuming that the affect does not change.
    public MyProgressNavigationAskIS(SessionManager smgr, PedagogicalModel pedagogicalModel) throws SQLException {
        super(smgr, pedagogicalModel);
        state = new MyState(smgr);
    }

    public void init(SessionManager smgr, PedagogicalModel pedagogicalModel)  {
        configure();

    }

    private void configure() {
        Element config = this.getConfigXML();
        if (config != null) {
            List<Element> intervalCritiaElts = config.getChildren("intervalCriteria");
            for (Element elt: intervalCritiaElts) {
                String t = elt.getAttributeValue("type");
                String v = elt.getAttributeValue("val");

                if (t.equals("affect")) {
                    checkEmotions=true;
                    String em = elt.getAttributeValue("emotion");
                    String lb = elt.getAttributeValue("lowerBound");
                    String ub = elt.getAttributeValue("upperBound");
                    Bounds b = new Bounds(lb,ub);
                    emotionSettings.put(em,b);
                }

            }
            Element elt = config.getChild("dialogType");
            String dType = "true";
            if (elt != null)
                 dType = elt.getAttributeValue("ask");
            this.isAskDialog = Boolean.parseBoolean(dType);
            elt = config.getChild("minIntervalBetweenMPPQueriesBasedOnAffect");
            if (elt != null) {
                String minutes = elt.getText();
                this.minIntervalBetweenMPPQueriesBasedOnAffect = Integer.parseInt(minutes) * 60 * 1000;
            }

        }
    }

    @Override
    /**
     * The pedagogy configures the behavior of this selector with elements like:
     *
     * a <intervalCriteria type="numProblems" val="6"/>
     b <intervalCriteria type="time" val="10"/>
     c <intervalCriteria type="affect" emotion="Interest" lowerbound="3" upperbound="5"/>
     d <intervalCriteria type="affect" emotion="Confidence"  upperbound="3"/>

     You can include all three, or as few as 1.   The meaning is whichever of a or b occurs first (number of probs == 6 or time == 10 min)
     and then MPP is displayed for 1 problem.   If c is included, then whenever this condition arises (interest>= 3 <= 5) we show MPP for 1 problem.
     If other emotions are checked (d) we can see that whenever confidence is <= 3 we will show MPP.

     THe flaw with the emotions stuff is that it works off reported emotions which only change when the user is prompted about a specific emotion.
     So if this finds that confidence was reported to be 2 and we have a condition that says show the MPP if confidence <= 3,  this intervention
     will keep showing the MPP until the user is again queried about confidence AND he raises his confidence level.   So there is an interrelationship
     between the use of this intervention and the AskEmotion intervention (and presumably the tutor which would attempt to ameliorate the emotion that
     is out of whack)
     */
    public NextProblemIntervention selectIntervention(NextProblemEvent e) throws Exception {
        long now = System.currentTimeMillis();

        // We only want this intervention to come up after a practice problem because the MPP return-to-hut will break unless the problem was originally practice.
        if (!smgr.getStudentState().getCurProblemMode().equals(Problem.PRACTICE))
            return null;
        NextProblemIntervention intervention=null;

        long lastInterventionForEmotions = state.getTimeOfLastInterventionForEmotions();
        long timeSinceLastEmotionMPPIntervention = now - lastInterventionForEmotions;
        if (checkEmotions && timeSinceLastEmotionMPPIntervention>= minIntervalBetweenMPPQueriesBasedOnAffect) {
            StudentModel sm = smgr.getStudentModel();

            if (sm instanceof AffectStudentModel  ) {
                AffectStudentModel asm =  ((AffectStudentModel) sm);
                for (String emotion : emotionSettings.keySet() ) {
                    int emVal=0;  // note that Student Model returns 0 for an emotion until student reports a value
                    if (emotion.equals(AffectStudentModel.CONFIDENT))
                        emVal = asm.getReportedConfidence();
                    else if (emotion.equals(AffectStudentModel.FRUSTRATED))
                        emVal = asm.getReportedFrustration();
                    else if (emotion.equals(AffectStudentModel.INTERESTED))
                        emVal = asm.getReportedInterest();
                    else if (emotion.equals(AffectStudentModel.EXCITED))
                        emVal = asm.getReportedExcitement();
                    Bounds b = emotionSettings.get(emotion);
                    if (emVal != 0 && b.within(emVal)) {
                        state.setTimeOfLastInterventionForEmotions(now);
                        if (this.isAskDialog)
                            intervention = new MyProgressNavigationAskIntervention();
                        else intervention =new MyProgressNavigationIntervention();
                        break;
                    }
                }
            }
        }
        if (intervention != null) {
            rememberInterventionSelector(this);
        }

        return intervention;
    }

    @Override
    public Intervention processContinueNextProblemInterventionEvent(ContinueNextProblemInterventionEvent e) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Intervention processInputResponseNextProblemInterventionEvent(InputResponseNextProblemInterventionEvent e) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }



    private class MyState extends State {
        private final String TIME_OF_LAST_INTERVENTION_FOR_EMOTIONS =  MyProgressNavigationAskIS.this.getClass().getSimpleName() + ".TimeOfLastInterventionForEmotions";
        private final String LAST_INTERVENTION_INDEX =  MyProgressNavigationAskIS.this.getClass().getSimpleName() + ".LastInterventionIndex";

        int lastInterventionIndex; // keeps track of the index of the last Affect we asked about
        long timeOfLastInterventionForEmotions;

        MyState (SessionManager smgr) throws SQLException {

            this.conn=smgr.getConnection();
            this.objid = smgr.getStudentId();
            WoProps props = smgr.getStudentProperties();
            Map m = props.getMap();
            lastInterventionIndex =  mapGetPropInt(m, LAST_INTERVENTION_INDEX, -1);
            timeOfLastInterventionForEmotions =  mapGetPropLong(m, TIME_OF_LAST_INTERVENTION_FOR_EMOTIONS, 0);
//            if (timeOfLastIntervention ==0)
//                setTimeOfLastIntervention(System.currentTimeMillis());

        }

        private int getLastInterventionIndex() {
            return lastInterventionIndex;
        }

        private void setLastInterventionIndex(int lastInterventionIndex) throws SQLException {
            this.lastInterventionIndex = lastInterventionIndex;
            setProp(this.objid,LAST_INTERVENTION_INDEX,lastInterventionIndex);
        }

        private long getTimeOfLastInterventionForEmotions() {
            return timeOfLastInterventionForEmotions;
        }

        private void setTimeOfLastInterventionForEmotions(long timeOfLastInterventionForEmotions) throws SQLException {
            this.timeOfLastInterventionForEmotions = timeOfLastInterventionForEmotions;
            setProp(this.objid,TIME_OF_LAST_INTERVENTION_FOR_EMOTIONS,timeOfLastInterventionForEmotions);
        }
    }

    private class Bounds {
        int lower=AffectStudentModel.EMOTION_LOWER_BOUND;
        int upper=AffectStudentModel.EMOTION_UPPER_BOUND;

        Bounds(String lower, String upper) {
            if (lower != null)
                this.lower = Integer.parseInt(lower);
            if (upper != null)
                this.upper = Integer.parseInt(upper);
        }

        int getLower() {
            return lower;
        }

        int getUpper() {
            return upper;
        }

        boolean within (int x) {
            return x >= lower && x <= upper;
        }
    }
}
