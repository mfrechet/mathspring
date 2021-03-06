package edu.umass.ckc.wo.db;

import edu.umass.ckc.wo.beans.PretestPool;
import edu.umass.ckc.wo.content.PrePostProblemDefn;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: marshall
 * Date: Mar 9, 2009
 * Time: 11:23:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class DbPrePost {

        /**
       * Given a problemId return a PrePostProblem object or null if it doesn't exist
       * @param conn
       * @param probId
       * @return
       * @throws SQLException
       */
      public static PrePostProblemDefn getPrePostProblem (Connection conn, int probId) throws SQLException {
          PreparedStatement ps = null;
          ResultSet rs = null;
          try {
//              String q = "select problemSet,name,url,description,answer,ansType,aChoice,bChoice,cChoice,dChoice,eChoice," +
              String q = "select id, name,url,description,answer,ansType,aChoice,bChoice,cChoice,dChoice,eChoice," +
                      "aURL,bURL,cURL,dURL,eURL from PrePostProblem where id=?";
              ps = conn.prepareStatement(q);
              ps.setInt(1, probId);
              rs = ps.executeQuery();
              if (rs.next()) {
//                  int problemSet = rs.getInt(1); // DM 3/09 don't want this anymore
                  int problemSet = -1; // give a bogus value.
                  int id = rs.getInt(1);       // added to take place of problemSet as item 1 which preserves #s below
                  String name = rs.getString(2);
                  String url = rs.getString(3);
                  if (rs.wasNull())
                      url = null;
                  String description = rs.getString(4);
                  if (rs.wasNull())
                      description = null;
                  String answer = rs.getString(5);
                  int ansType = rs.getInt(6);
                  String aURL = null, bURL = null, cURL = null, dURL = null, eURL = null;
                  String aChoice = null, bChoice = null, cChoice = null, dChoice = null, eChoice = null;
                  PrePostProblemDefn p;
                  if (ansType == PrePostProblemDefn.SHORT_ANSWER) {
                      ;
                  }
                  else {
                      aChoice = rs.getString(7);
                      if (rs.wasNull())
                          aChoice = null;
                      bChoice = rs.getString(8);
                      if (rs.wasNull())
                          bChoice = null;
                      cChoice = rs.getString(9);
                      if (rs.wasNull())
                          cChoice = null;
                      dChoice = rs.getString(10);
                      if (rs.wasNull())
                          dChoice = null;
                      eChoice = rs.getString(11);
                      if (rs.wasNull())
                          eChoice = null;
                      aURL = rs.getString(12);
                      if (rs.wasNull())
                          aURL = null;
                      bURL = rs.getString(13);
                      if (rs.wasNull())
                          bURL = null;
                      cURL = rs.getString(14);
                      if (rs.wasNull())
                          cURL = null;
                      dURL = rs.getString(15);
                      if (rs.wasNull())
                          dURL = null;
                      eURL = rs.getString(16);
                      if (rs.wasNull())
                          eURL = null;

                  }
              return new PrePostProblemDefn(probId, name, description, url, ansType, answer, problemSet, aChoice, bChoice, cChoice,
                          dChoice, eChoice, aURL, bURL, cURL, dURL, eURL);
              }
          } catch (SQLException e) {
              e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
          } finally {
              if (rs != null)
                  rs.close();
              if (ps != null)
              ps.close();
          }
          return null;
      }



    public static List<PretestPool> getPools(Connection conn) throws SQLException {
        List<PretestPool> pools = new ArrayList<PretestPool>();
        // check for id > 0 is way to avoid a sentinel value in the first row (id =0)
        String q = "select id,description from prepostpool where isActive=1 and id>0";
        PreparedStatement ps = conn.prepareStatement(q);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int id = rs.getInt(1);
            String descr = rs.getString(2);
            pools.add(new PretestPool(id,descr));
        }
        return pools;
    }

    public static PretestPool getPretestPool (Connection conn, int classId) throws SQLException {
        String q = "select pretestPoolId from class where Id=?";
        PreparedStatement ps = conn.prepareStatement(q);
        ps.setInt(1,classId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            int poolid = rs.getInt(1);
            q = "select description from prepostpool where id=?";
            PreparedStatement ps2  = conn.prepareStatement(q);
            ps2.setInt(1,poolid);
            ResultSet rs2 = ps2.executeQuery();
            if (rs2.next()) {
                String descr = rs2.getString(1);
                return new PretestPool(poolid,descr);
            }
            else return null;
        }
        else return null;

    }

    public static List<PretestPool> getAllPretestPools (Connection conn) throws SQLException {
        List<PretestPool> pools = DbPrePost.getPools(conn);
        return pools;
    }

    /**
     * Get all the prepost test ids that are in the given pool
     * @param conn
     * @param poolId
     * @return
     * @throws SQLException
     */
    public static List<Integer> getTestsInPool (Connection conn, int poolId) throws SQLException {
        ResultSet rs=null;
        PreparedStatement stmt=null;
        try {
            List<Integer> testIds = new ArrayList<Integer>();
            String q = "select id from preposttest where poolId=?";
            stmt = conn.prepareStatement(q);
            stmt.setInt(1,poolId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                int testId= rs.getInt(1);
                testIds.add(testId);
            }
            return testIds;
        }
        finally {
            if (stmt != null)
                stmt.close();
            if (rs != null)
                rs.close();
        }
    }
}
