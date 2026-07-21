import java.sql.*;

public class DeleteUser {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/buffturf_db?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true";
        String user = "root";
        String password = "hpvictus";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, user, password);
            Statement stmt = conn.createStatement();
            int rows = stmt.executeUpdate("DELETE FROM users WHERE email = 'jshinde0105@gmailc.om'");

            System.out.println("Deleted " + rows + " user(s).");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
