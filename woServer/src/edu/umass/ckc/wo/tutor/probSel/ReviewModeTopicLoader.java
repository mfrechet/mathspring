package edu.umass.ckc.wo.tutor.probSel;

/**
 * Created with IntelliJ IDEA.
 * User: marshall
 * Date: 11/26/12
 * Time: 3:12 PM
 * To change this template use File | Settings | File Templates.
 */

import edu.umass.ckc.wo.cache.ProblemMgr;
import edu.umass.ckc.wo.content.Problem;
import edu.umass.ckc.wo.smgr.SessionManager;
import edu.umass.ckc.wo.smgr.StudentState;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;


/**
 * Created with IntelliJ IDEA.
 * User: marshall
 * Date: 11/20/12
 * Time: 3:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReviewModeTopicLoader extends TopicLoader {
    private static Logger logger = Logger.getLogger(ReviewModeTopicLoader.class);





    public ReviewModeTopicLoader(Connection conn, int classid, SessionManager smgr) {
        super(conn, classid, smgr);
    }

    /**
      * Goes through the list of problems in the topic and eliminates some based on the mode being "challenge".
     * Here's what gets eliminated:
     *  problems omitted by the class lesson plan
     *  problems that have a difficulty level which is less than 75% of students mastery level in the topic  (controlled by
     *  the PERCENT_OF_MASTERY constant above).
     *  In addition it looks at the last encounter a student had with a problem in this topic and if the student required
     *  significant help or made a significant number of mistakes in answering it, then it will be included in the set of
     *  problems given in challenge mode.
      *
     * @param smgr
     * @param resetCounters
     * @throws java.sql.SQLException
      */
     public List<Problem> prepareForSelection(SessionManager smgr, boolean resetCounters) throws SQLException {
         // studentID and classID were set in init method.
         StudentState state = smgr.getStudentState();
         topicID =state.getCurTopic();
         this.topicName = ProblemMgr.getTopic(this.topicID).getName();
         classID = determineClass();  // get either the default class (with default lesson plan) or the actual class (with a custom plan)
         probsInTopic =  ProblemMgr.getTopicProblems(topicID);

          return probsInTopic;
     }
}
