import org.json.JSONObject;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

import static spark.Spark.*;

public class Main {
    public static void main(String[] args) {
        staticFiles.externalLocation("/var/www/html");
        staticFiles.expireTime(600);
        get("/hello", (req, res) -> "Hello World??????????");
        get("/search/:type", (request, response) -> {
            response.type("application/json");
            //System.out.println("SELECT * FROM githubapi_tbl WHERE type=\'"+request.params("type")+"\' AND q=\'"+request.queryParams("q")+"\' AND o=\'"+request.queryParams("o")+"\' AND s=\'"+request.queryParams("s")+"\';");
            MySQLClient mysql = new MySQLClient();
            ResultSet rs = mysql.executeQuery("SELECT * FROM githubapi_tbl WHERE type=\'"+request.params("type")+"\' AND q=\'"+request.queryParams("q")+"\' AND o=\'"+request.queryParams("o")+"\' AND s=\'"+request.queryParams("s")+"\';");
            if(rs.next()) {
                JSONObject json = new JSONObject(rs.getString("json"));
                //mysql.closeConnection();
                rs.close();
                return json.toString(4);
            }else {
                String content = getHttpsURLContent("https://api.github.com/search/"+request.params("type")+"?"+request.raw().getQueryString());
                if(content != null){
                    JSONObject json = new JSONObject(content);
                    JSONObject simplifiedJson = new JSONObject();
                    simplifiedJson.put("total_count",json.getInt("total_count"));
                    mysql.execute("BEGIN;");
                    if( mysql.execute("INSERT INTO githubapi_tbl (type,q,o,s,json) VALUES (\'"+request.params("type")+"\',\'"+request.queryParams("q")+"\',\'"+request.queryParams("o")+"\',\'"+request.queryParams("s")+"\',\'"+newStrWithEscapeCharsOf(simplifiedJson.toString())+"\');") ){
                        mysql.execute("COMMIT;");
                    }else{
                        //System.out.println("mission failed");
                        mysql.execute("ROLLBACK;");
                    }
                    //mysql.closeConnection();
                    return simplifiedJson.toString(4);
                } else{
                    //mysql.closeConnection();
                    response.status(500);
                    return null;
                }
            }
        });

        startUpdateDB();
    }

    public static String getHttpsURLContent(String surl) {
        try {
            String content = null;
            //创建SSLContext
            SSLContext sslContext=SSLContext.getInstance("SSL");
            TrustManager[] tm={new MyX509TrustManager()};
            //初始化
            sslContext.init(null, tm, new java.security.SecureRandom());;
            //获取SSLSocketFactory对象
            SSLSocketFactory ssf=sslContext.getSocketFactory();
            URL url = new URL(surl);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");   //设置本次请求的方式 ， 默认是GET方式， 参数要求都是大写字母
            conn.setConnectTimeout(5000);//设置连接超时
            conn.setDoInput(true);//是否打开输入流 ， 此方法默认为true
            conn.setUseCaches(false);
            //设置当前实例使用的SSLSoctetFactory
            conn.setSSLSocketFactory(ssf);
            conn.connect();//表示连接

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String lines;
                StringBuffer sbf = new StringBuffer();
                while ((lines = reader.readLine()) != null) {
                    lines = new String(lines.getBytes(), "utf-8");
                    sbf.append(lines);
                }
                content = sbf.toString();
            }
            conn.disconnect();
            return content;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static String newStrWithEscapeCharsOf(String s){
        StringBuffer sb = new StringBuffer(s);
        int i;
        char c;
        for(i=0;i<sb.length();++i){
            c = sb.charAt(i);
            if(c=='\'' || c=='\"' || c=='\\'){
                sb.insert(i,'\\');
                ++i;
            }
        }
        return sb.toString();
    }

    public static void startUpdateDB(){
        int interval = 600;//second
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                MySQLClient mysql = new MySQLClient();
                ResultSet rs = mysql.executeQuery("SELECT id,type,q,o,s FROM githubapi_tbl;");
                String surl = null;
                try {
                    while (rs.next()) {
                        try {
                            surl = "https://api.github.com/search/" + rs.getString("type") +
                                    "?q=" + URLEncoder.encode(rs.getString("q"), "UTF-8") +
                                    "&o=" + URLEncoder.encode(rs.getString("o"), "UTF-8") +
                                    "&s=" + URLEncoder.encode(rs.getString("s"), "UTF-8");
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                        String content = getHttpsURLContent(surl);
                        while(content == null){
                            try{
                                System.out.println("sleep");
                                Thread.currentThread().sleep(60000);
                            }catch(Exception e){
                                e.printStackTrace();
                            }
                            content = getHttpsURLContent(surl);
                        }
                        System.out.println("https url access successful.surl="+surl);
                        JSONObject json = new JSONObject(content);
                        JSONObject simplifiedJson = new JSONObject();
                        simplifiedJson.put("total_count",json.getInt("total_count"));
                        mysql.execute("BEGIN;");
                        if(mysql.execute("UPDATE githubapi_tbl SET json=\'"+newStrWithEscapeCharsOf(simplifiedJson.toString())+"\' WHERE id="+rs.getInt("id")+";")){
                            mysql.execute("COMMIT;");
                        }else{
                            mysql.execute("ROLLBACK;");
                            System.out.println("UPDATE FAILED");
                        }
                    }
                    rs.close();
                }catch(SQLException se){
                    se.printStackTrace();
                }
            }
        },0,interval*1000);
    }

}