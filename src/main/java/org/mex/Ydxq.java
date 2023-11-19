package org.mex;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

public class Ydxq {

    /**
     * @param cookie 用户cookie
     * @param aimScore 目标成绩
     * @param gender 用户性别 1男 2女
     * @param heart 过程间隔包
     * @param RandomHeartScore 是否随机过程成绩
     *                         true: 随机(0-heartScore)
     *                         false: 固定值heartScore
     * @param heartScore 过程间隔包成绩
     * @param UA 用户UA
     */
    private String cookie;
    private HashMap<String, String> cookieMap = new HashMap<>();
    private int aimScore = 10000;
    private int gender = 2; // 1男 2女
    private int heart = 6; // 过程间隔包
    private boolean RandomHeartScore = true; // 随机过程成绩
    private int heartScore = 30; // 过程间隔包成绩
    private String UA = "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.6045.123 Mobile Safari/537.36";
    private String account_id; // 用户id 在cookie中获取

    private String URL = "https://api-takumi.mihoyo.com/event/e20220908jump/game_report";

    Ydxq(String cookie, int aimScore) {
        this.cookie = cookie;
        this.aimScore = aimScore;
        String[] cookieList = cookie.split(";");
        for (String s : cookieList) {
            String[] kv = s.split("=");
            cookieMap.put(kv[0].trim(), kv[1].trim());
        }
        this.account_id = cookieMap.get("account_id");
    }

    Ydxq(String cookie, int aimScore, int gender, int heart, boolean RandomHeartScore, int heartScore, String UA) {
        this.cookie = cookie;
        this.aimScore = aimScore;
        this.gender = gender;
        this.heart = heart;
        this.RandomHeartScore = RandomHeartScore;
        this.heartScore = heartScore;
        this.UA = UA;
        String[] cookieList = cookie.split(";");
        for (String s : cookieList) {
            String[] kv = s.split("=");
            cookieMap.put(kv[0].trim(), kv[1].trim());
        }
        this.account_id = cookieMap.get("account_id");
    }

    public void run() {

        // 当前成绩
        int nowScore = 0;
        boolean isStart = true;
        String transaction = null;
        do {
            // 类型
            byte report_type = isStart ? (byte) 0 : nowScore < this.aimScore ? (byte) 1 : (byte) 2;
            isStart = false;
            System.out.println(" ## " + nowScore + " of " + this.aimScore + " ##");
            // 生成过程包
            String param = make_param((byte) report_type, nowScore, transaction);
            // 发送过程包
            String result = post(this.URL + "?lang=zh-cn&plat=PT_WEB&user_id=" + this.account_id, param, this.cookie, this.UA).trim();
            System.out.println(" <= " + param + "\n => " + result + "\n");
            // 获取transaction
//            {"retcode":0,"message":"OK","data":{"transaction":"320921207-cn-sh-aliyun-plat-e20220908jump-3-3UG4JuxEG","has_unfinished_game":false,"info":{"cur_points":0,"highest_points":1001,"total_points":20000,"new_record":false}}}
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(result, JsonObject.class);
            transaction = jsonObject.get("data").getAsJsonObject().get("transaction").getAsString();
            // 下次成绩增加
            int HeartScore = this.RandomHeartScore ? (int) (Math.random() * this.heartScore) : this.heartScore;
            // 更新当前成绩
            nowScore += HeartScore;
            // 休眠
            try {
                Thread.sleep(this.heart * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (nowScore < this.aimScore);
    }

    /**
     * 生成过程包
     * @param report_type 游戏类型 0开始 1过程 2结束
     *                     0: RT_Start
     *                     1: RT_Midway
     *                     2: RT_End
     * @param nowScore 当前成绩
     * @param transaction 交易号
     */
    public String make_param(byte report_type, int nowScore, String transaction){
        String gameType = "";
        switch (report_type) {
            case 0 -> gameType = "RT_Start";
            case 1 -> gameType = "RT_Midway";
            case 2 -> gameType = "RT_End";
        }

        // 获取时间戳1700378280
        long time = System.currentTimeMillis() / 1000;

        // RT_Midway_320921207_1700378280_24_PT_WEBx7afabqgcjw1FJ
        String text = gameType + "_" + this.account_id + "_" + time + "_" + nowScore + "_PT_WEBx7afabqgcjw1FJ";
        // md5
        String sign = DigestUtils.md5Hex(text);
        System.out.println("status: " + text + "\tmd5: " + sign);
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        // {"gender":2,"points":24,"report_type":"RT_Midway","timestamp":1700376858,"transaction":"320921207-cn-sh-aliyun-plat-e20220908jump-3-3UEcx4Jps","encryption":"34ed884676c4d165fee69bad0ce5892d","plat":"PT_WEB"}
        jsonObject.addProperty("gender", this.gender);
        jsonObject.addProperty("points", nowScore);
        jsonObject.addProperty("report_type", gameType);
        jsonObject.addProperty("timestamp", time);
        jsonObject.addProperty("transaction", transaction==null ? this.account_id+"-cn-sh-aliyun-plat-e20220908jump-3-3UEcx4Jps" : transaction);
        jsonObject.addProperty("encryption", sign);
        jsonObject.addProperty("plat", "PT_WEB");
        return gson.toJson(jsonObject);
    }

    //post请求
    public static String post(String url, String param, String cookie, String UA) {
        PrintWriter out = null;
        BufferedReader in = null;
        StringBuilder result = new StringBuilder();
        try {
            URL realUrl = new URL(url);
            //打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            //设置通用的请求属性
            conn.setRequestProperty("Cookie", cookie);
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Connection", "close");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Host", "www.youdao.com");
            conn.setRequestProperty("Origin", "https://webstatic.mihoyo.com");
            conn.setRequestProperty("Referer", "https://webstatic.mihoyo.com/");
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
            conn.setRequestProperty("Sec-Ch-Ua-Platform", "Android");
            //发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            //获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            //发送请求参数
            out.print(param);
            //flush输出流的缓冲
            out.flush();
            //定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "utf-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result.append("\n").append(line);
            }
        } catch (Exception e) {
            System.out.println("发送POST请求出现异常！" + e);
        }
        //使用finally块来关闭输出流、输入流
        finally {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
            } catch (IOException ex) {
                //ex.printStackTrace();
            }
        }
        return result.toString();
    }
}
