package edu.umass.ckc.wo.db;

import edu.umass.ckc.wo.tutor.Settings;
import edu.umass.ckc.wo.tutor.studmod.StudentProblemData;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Created by IntelliJ IDEA.
 * User: marshall
 * Date: 6/11/12
 * Time: 2:37 PM
 * To change this template use File | Settings | File Templates.
 *
 * Methods for accessing the studentProblemHistory table
 *
 *  The assumption for now is that each time a problem is presented, a new row gets inserted in this table.
 *  So if a problem is presented and is then skipped and then presented again later there will be two entries in this table.
 *
 *  Modified version of Dovan's table problemEventLog    (- in front means "don't need")
(
   studID  int(10),
   sessionID int(10),
   problemID int(10),\\
   topicID int(10),
   problemBeginTime timestamp,
   problemEndTime timestamp,
   timeInSession int(10),
   timeInTutor int(10),
   timeToFirstAttempt int(10),
   timeToFirstHint
   timeToSolve int(10),
   numAttempts
   numHints int(3),
   numHelpAids int(3),
   numAttemptsTillSolve
   seenAllHints tinyhint
   Mode varchar(20),  // example, intro, practice
   Mastery int(10), // mastery after problem ends (may or may not be solved)
   emotionAfter  varchar(20),
   emotionLevel int(3),
   studentState varchar(20) )
 *
 *
 */
public class DbStudentProblemHistory {

    public static final String NUM_MISTAKES = "numMistakes";
    public static final String NUM_ATTEMPTS_TO_SOLVE = "numAttemptsToSolve";
    public static final String NUM_HINTS_BEFORE_SOLVE = "numHintsBeforeSolve";
    public static final String NUM_HINTS = "numHints";
    public static final String IS_SOLVED = "isSolved";
    public static final String TIME_TO_SOLVE = "timeToSolve";
    public static final String TIME_TO_FIRST_ATTEMPT = "timeToFirstAttempt";
    public static final String TIME_TO_FIRST_HINT = "timeToFirstHint";
    public static final String SOLUTION_HINT_GIVEN = "solutionHintGiven";

    private static Logger logger =   Logger.getLogger(DbStudentProblemHistory.class);


    public static final String SPHTBL = "studentProblemHistory";


    /** Called when a problem begins.   If this is the first encounter with the problem, it creates the row in the studentProblemHistory.
     * If this is a return to a problem that was previously unsolved (the only reason for  */
    public static int beginProblem(Connection conn, int sessId, int studId, int probId, int topicId, long startTime,
                                   long timeInSession, long timeInTutor, String mode) throws SQLException {
        ResultSet rs=null;
        PreparedStatement stmt=null;
        try {


            String q = "insert into " +SPHTBL+
                    " (problemID, studID, sessionID, topicID, problemBeginTime, timeInTutor, timeInSession, mode) " +
                    "values (?,?,?,?,?,?,?,?)";
            stmt = conn.prepareStatement(q, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1,probId);
            stmt.setInt(2, studId);
            stmt.setInt(3, sessId);
            stmt.setInt(4, topicId);
            stmt.setTimestamp(5, new Timestamp(startTime));
            stmt.setLong(6, timeInTutor);
            stmt.setLong(7,timeInSession);
            stmt.setString(8,mode);
            stmt.execute();
            rs = stmt.getGeneratedKeys();
            rs.next();
            return rs.getInt(1);
        }
        catch (SQLException e) {
            System.out.println(e.getErrorCode());
            if (e.getErrorCode() == Settings.duplicateRowError ||e.getErrorCode() == Settings.keyConstraintViolation )
                return -1;
            else throw e;
        }
        finally {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
        }
    }



    /**
     * Get the last entry for the student.   Thats the record s/he is presumed to be working on.
     * @param conn
     * @param studId
     * @return
     */
    public static int getMostRecentStudentProblemHistoryRecord (Connection conn, int studId) throws SQLException {
        ResultSet rs=null;
        PreparedStatement stmt=null;
        try {
            String q = "select max(id) from " +SPHTBL+ " where studid=?";
            stmt = conn.prepareStatement(q);
            stmt.setInt(1,studId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                int c= rs.getInt(1);
                return c;
            }
            else return -1;
        }
        finally {
            if (stmt != null)
                stmt.close();
            if (rs != null)
                rs.close();
        }
    }


    /** When the problem ends, the student has either solved it or has given up.   If he is giving up,  this record may be updated
     * in the future if the problem is resumed.
     *
     *
     * @param conn
     * @param historyRecId
     * @param numHintsBeforeSolve
     * @param numAttemptsToSolve
     * @param timeToSolve
     * @param timeToFirstHint
     * @param timeToFirstAttempt
     * @param isCorrect
     * @param problemEndTime
     * @param numMistakes
     * @param numHints
     * @param solutionHintGiven
     * @param mastery
     * @param effort     @return
     * @param seenVideo
     * @param seenExample
     * @param textReaderUsed @throws SQLException                                       */
    public static int endProblem(Connection conn, int historyRecId, int numHintsBeforeSolve, int numAttemptsToSolve,
                                 long timeToSolve, long timeToFirstHint, long timeToFirstAttempt, boolean isCorrect,
                                 long problemEndTime, int numMistakes, int numHints,
                                 boolean solutionHintGiven, double mastery,
                                 String effort, boolean seenVideo, int examplesSeen, boolean textReaderUsed) throws SQLException {

        PreparedStatement ps=null;
        try {
            String q = "update " +SPHTBL+ " " +
                    "set problemEndTime=?, " +
                    "timeToFirstAttempt=?, " +
                    "timeToFirstHint=?, " +
                    "timeToSolve=?, " +
                    "numMistakes=?, " +
                    "numHints=?, " +
                    "videoSeen=?, " +
                    "numAttemptsToSolve=?, " +
                    "solutionHintGiven=?, " +
                    "mastery=?, " +
                    "effort=?, " +
                    "exampleSeen=?, " +
                    "textReaderUsed=?, " +
                    "numHintsBeforeSolve=?, " +
                    "problemEndTime=?," +
                    "isSolved=? " +
                    "where id=?";
            ps = conn.prepareStatement(q);
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setLong(2, timeToFirstAttempt);
            ps.setLong(3, timeToFirstHint);
            ps.setLong(4, timeToSolve);
            ps.setInt(5, numMistakes);
            ps.setInt(6, numHints);
            ps.setBoolean(7, seenVideo);
            ps.setInt(8, numAttemptsToSolve);
            ps.setBoolean(9, solutionHintGiven);
            ps.setDouble(10, mastery);
            ps.setString(11, effort);
            ps.setBoolean(12, examplesSeen>0);   // DB column needs to be changed to Integer to put the actual count in if anyone ever wants it
            ps.setBoolean(13, textReaderUsed);
            ps.setInt(14, numHintsBeforeSolve);
            ps.setTimestamp(15, new Timestamp(problemEndTime));
            ps.setBoolean(16, isCorrect);
            ps.setInt(17, historyRecId);

            return ps.executeUpdate();
        } finally {
            if (ps != null)
                ps.close();
        }

    }

    public static int updateVar(int id, Connection conn, String varName, Object value) throws SQLException {
        PreparedStatement stmt = null;
        try {
            String q = "update " +SPHTBL+ " " +
                    "set " + varName + "=? where id=?";
            stmt = conn.prepareStatement(q);
            if (value instanceof Long)
                stmt.setLong(1, (Long) value);
            if (value instanceof Boolean)
                stmt.setBoolean(1, (Boolean) value);
            else if (value instanceof Integer)
                stmt.setInt(1, (Integer) value);
            else if  (value instanceof String)
                stmt.setString(1, (String) value);
            stmt.setInt(2,id)  ;
            return stmt.executeUpdate();
        } finally {
            if (stmt != null)
                stmt.close();
        }
    }

    public static int updateEmotions(Connection conn, int id, String emotion, int level) throws SQLException {
        PreparedStatement stmt=null;
        try {
            String q = "update " +SPHTBL+ " set emotionAfter=?, emotionLevel=? where id=?";
            stmt = conn.prepareStatement(q);
            stmt.setString(1,emotion);
            stmt.setInt(2,level);
            stmt.setInt(3,id);
            return stmt.executeUpdate();
        } finally {
            if (stmt != null)
                stmt.close();
        }
    }



    public static void loadHistory(Connection conn, int studId, List<StudentProblemData> history) throws SQLException {
        ResultSet rs=null;
        PreparedStatement ps=null;
        try {
            String q = "select ID,sessionId,problemId,topicId,problemBeginTime,problemEndTime,timeInSession," +
                    "timeInTutor,timeToFirstAttempt,timeToFirstHint,timeToSolve,numMistakes,numHints,videoSeen," +
                    "numAttemptsToSolve,solutionHintGiven,mode,mastery,emotionAfter,emotionLevel,effort,exampleSeen," +
                    "textReaderUsed,numHintsBeforeSolve,isSolved from " +SPHTBL+ " where studId=? order by ID";
            ps = conn.prepareStatement(q);
            ps.setInt(1,studId );

            rs = ps.executeQuery();
            while (rs.next()) {
                StudentProblemData d = new StudentProblemData();
                d.setId(rs.getInt("ID"));
                d.setSessId(rs.getInt("sessionId"));
                d.setProbId(rs.getInt("problemId"));
                d.setTopicId(rs.getInt("topicId"));
                d.setProblemBeginTime(rs.getTimestamp("problemBeginTime").getTime());
                Timestamp endTime = rs.getTimestamp("problemEndTime");
                if (!rs.wasNull())
                    d.setProblemEndTime(endTime.getTime());
                else d.setProblemEndTime(0);
                d.setTimeInSession(rs.getInt("timeInSession"));
                d.setTimeInTutor(rs.getInt("timeInTutor"));
                d.setTimeToFirstAttempt(rs.getInt("timeToFirstAttempt"));
                d.setTimeToFirstHint(rs.getInt("timeToFirstHint"));
                d.setTimeToSolve(rs.getInt("timeToSolve"));
                d.setNumMistakes(rs.getInt("numMistakes"));
                d.setNumHints(rs.getInt("numHints"));
                d.setSeenVideo(rs.getBoolean("videoSeen"));
                d.setNumAttemptsToSolve(rs.getInt("numAttemptsToSolve"));
                d.setGivenAnswerHint(rs.getBoolean("solutionHintGiven"));
                d.setMode(rs.getString("mode"));
                d.setMastery(rs.getDouble("mastery"));
                d.setEmotion(rs.getString("emotionAfter"));
                d.setEmotionLevel(rs.getInt("emotionLevel"));
                d.setEffort(rs.getString("effort"));
                d.setSeenExample(rs.getBoolean("exampleSeen"));
                d.setUsedTextReader(rs.getBoolean("textReaderUsed"));
                d.setNumHintsBeforeCorrect(rs.getInt("numHintsBeforeSolve"));
                d.setSolved(rs.getBoolean("isSolved"));
                history.add(d);
            }
        }
        finally {
            if (ps != null)
                ps.close();
            if (rs != null)
                rs.close();
        }

    }

}