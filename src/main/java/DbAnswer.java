package src.main.java;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.apache.log4j.Logger;

public class DbAnswer {
    private static final Logger log = Logger.getLogger(DbAnswer.class.getName());

    public long id = 0;
    public long clientId;
    public long questionId;
    public long optionChoiceId;
    public int answerNumeric;
    public String answerText;
    public boolean answerYN;
    public Date created;

    public DbAnswer(long clientId, long questionId) {
        this.clientId = clientId;
        this.questionId = questionId;
    }

    public void insert(Connection conn) {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("INSERT INTO answer (clientId, questionId, optionChoiceId, ");
            sb.append("answerNumeric, answerText, answerYN, created) ");
            sb.append("VALUES (?, ?, ?, ?, ?, ?, ?)");

            PreparedStatement ps = conn.prepareStatement(sb.toString(),
                                                         Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, this.clientId);
            ps.setLong(2, this.questionId);
            ps.setLong(3, this.optionChoiceId);
            ps.setInt(4, this.answerNumeric);
            ps.setString(5, this.answerText);
            ps.setBoolean(6, this.answerYN);
            Date now = new Date();
            java.sql.Date sqlDate = new java.sql.Date(now.getTime());
            ps.setDate(7, sqlDate);

            int out = ps.executeUpdate();
            if (out == 0) {
                log.info("Failed to insert answer record!");
            }
            else {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    this.id = rs.getLong(1);
                }
            }
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbAnswer.insert(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbAnswer.insert(): " + e);
             e.printStackTrace();
        }
    }
}
