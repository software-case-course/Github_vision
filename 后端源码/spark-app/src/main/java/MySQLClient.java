import java.sql.*;
public class MySQLClient {
    // JDBC 驱动名及数据库 URL
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost:3306/test";
    // 数据库的用户名与密码，需要根据自己的设置
    static final String USER = "root";
    static final String PASS = "123456";

    private Connection conn = null;
    private Statement stmt = null;

    MySQLClient() {
        connect();
    }

    @Override
    protected void finalize() throws Throwable {
        closeConnection();
        super.finalize();
    }


    private void connect(){
        try{
            System.out.println("连接数据库...");
            conn = DriverManager.getConnection(DB_URL,USER,PASS);
            System.out.println(" 实例化Statement对象...");
            stmt = conn.createStatement();
        }catch(SQLException se){
            se.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public boolean execute(String sql){//用于除了select的其他数据库操作，返回true表示操作成功
        try{
            stmt.execute(sql);
            return true;
        }catch(SQLException se){
            se.printStackTrace();
            return false;
        }
    }

    public ResultSet executeQuery(String sql){//只能执行select
        try{
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            return rs;
        }catch(SQLException se){
            se.printStackTrace();
            return null;
        }
    }

    public void closeConnection(){
        try{
            if(stmt!=null) stmt.close();
        }catch(SQLException se2){
        }// 什么都不做
        try{
            if(conn!=null) conn.close();
        }catch(SQLException se){
            se.printStackTrace();
        }
    }
  /* public static void test() {
        try{
// 注册 JDBC 驱动
            Class.forName("com.mysql.jdbc.Driver");
// 打开链接
            System.out.println("连接数据库...");
            conn = DriverManager.getConnection(DB_URL,USER,PASS);
// 执行查询
            System.out.println(" 实例化Statement对象...");
            stmt = conn.createStatement();
            String sql;
            sql = "SELECT id, name, url FROM websites";
            ResultSet rs = stmt.executeQuery(sql);
// 展开结果集数据库
            while(rs.next()){
// 通过字段检索
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String url = rs.getString("url");
// 输出数据
                System.out.print("ID: " + id);
                System.out.print(", 站点名称: " + name);
                System.out.print(", 站点 URL: " + url);
                System.out.print("\n");
            }
// 完成后关闭
            rs.close();
            stmt.close();
            conn.close();
        }catch(SQLException se){
// 处理 JDBC 错误
            se.printStackTrace();
        }catch(Exception e){
// 处理 Class.forName 错误
            e.printStackTrace();
        }finally{
// 关闭资源
            try{
                if(stmt!=null) stmt.close();
            }catch(SQLException se2){
            }// 什么都不做
            try{
                if(conn!=null) conn.close();
            }catch(SQLException se){
                se.printStackTrace();
            }
        }
        System.out.println("Goodbye!");
    }*/
}
