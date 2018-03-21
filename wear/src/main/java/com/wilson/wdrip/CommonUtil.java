package com.wilson.wdrip;

import android.text.TextUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by wzhan025 on 3/15/2018.
 */


public class CommonUtil {

    public static String FORMAT_DATE_ISO="yyyy-MM-dd'T'HH:mm:ssZ";

    public static long getLongHtime(long...lTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        long longTime = System.currentTimeMillis();
        if (lTime != null) {
            if (lTime.length > 0)
                longTime = lTime[0];
        }
        String strTime = String.format("%s30:00", sdf.format(longTime).substring(0,14));

        try {
            Date tmpDate = sdf.parse(strTime);
            longTime = tmpDate.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return longTime;
    }
    public static long getLongMtime(long...lTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        long longTime = System.currentTimeMillis();
        if (lTime != null) {
            if (lTime.length > 0)
                longTime = lTime[0];
        }
        String strTime = String.format("%s00", sdf.format(longTime).substring(0,17));

        try {
            Date tmpDate = sdf.parse(strTime);
            longTime = tmpDate.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return longTime;
    }
    public static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        long longTime = System.currentTimeMillis();
        return sdf.format(longTime);
    }
    public static float fuzz(long...lTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        long longTime = System.currentTimeMillis();
        if (lTime != null) {
            if (lTime.length > 0)
                longTime = lTime[0];
        }
        long tTime = longTime;
        try {
            Date tmpDate = sdf.parse("2018/01/01 00:00:00");
            tTime = tmpDate.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return (longTime - tTime)/1000;
    }

    public static String getStringTime(Long... dLong) {
        SimpleDateFormat aDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String cc = "";
        if (dLong != null) {
            if (dLong.length > 0) {
                cc = aDateFormat.format(new Date(dLong[0]));
            }
            else {
                Long dTime = System.currentTimeMillis();
                cc = aDateFormat.format(dTime);
            }
        }
        return cc;
    }
    public static String getLabelTime(Long... lTime) {
        int intHour = 0;
        int intMinute = 0;
        String sTime = "";
        if (lTime != null) {
            if (lTime.length > 0) {
                sTime = getStringTime(lTime[0]);
            }
            else {
                sTime = getStringTime();
            }
            intHour = Integer.parseInt(subTimeHour(sTime));
            intMinute = Integer.parseInt(subTimeMinute(sTime));
        }

        return String.format("%s:%02d", intHour, (intMinute/30)*30);
    }

    public static String subTimeMonth(String time){
        return time.substring(4,6);
    }
    public static String subTimeDay(String time){
        return time.substring(6,8);
    }
    public static String subTimeHour(String time){
        return time.substring(8,10);
    }
    public static String subTimeMinute(String time){
        return time.substring(10,12);
    }
    public static String subTimeHM(String time) {
        return time.substring(8,12);
    }
    public static String subTimeHMS(String time) {
        return time.substring(10,14);
    }

    public static String bytesToHexString(Byte[] paramArrayOfByte)
    {
        StringBuilder localStringBuilder = new StringBuilder("");
        char[] arrayOfChar = new char[2];
        int j = paramArrayOfByte.length;
        for (int i = 0; i < j; i++)
        {
            int k = paramArrayOfByte[i];
            arrayOfChar[0] = Character.forDigit(k >>> 4 & 0xF, 16);
            arrayOfChar[1] = Character.forDigit(k & 0xF, 16);
            localStringBuilder.append(arrayOfChar);
        }
        return localStringBuilder.toString();
    }

    /**
     * 十六进制字符串转换成 ASCII字符串
     * @param hexStr String Byte字符串
     * @return String 对应的字符串
     */
    public static String hexStr2Str(String hexStr){
        String mHexStr = "0123456789ABCDEF";
        hexStr = hexStr.toString().trim().replace(" ", "").toUpperCase(Locale.US);
        char[] hexs = hexStr.toCharArray();
        byte[] bytes = new byte[hexStr.length() / 2];
        int iTmp = 0x00;;

        for (int i = 0; i < bytes.length; i++){
            iTmp = mHexStr.indexOf(hexs[2 * i]) << 4;
            iTmp |= mHexStr.indexOf(hexs[2 * i + 1]);
            bytes[i] = (byte) (iTmp & 0xFF);
        }
        return new String(bytes);
    }

    public static int getGlucoseRaw2(String paramString)
    {
        return Integer.parseInt(paramString, 16) & 0x3FFF;
    }
    public static double formatValue(int paramInt)
    {
        return new BigDecimal(paramInt / 180.0D).doubleValue();
    }
    public static double getBG(Byte[] paramArrayOfByte) {
        return formatValue(getGlucoseRaw2(bytesToHexString(paramArrayOfByte)));
    }

    public static Date fromISODateString(String isoDateString)
            throws Exception
    {
        SimpleDateFormat f = new SimpleDateFormat(FORMAT_DATE_ISO);
        return f.parse(isoDateString);
    }

    public static String toISOString(Date date, String format, TimeZone tz)
    {
        if( format == null ) format = FORMAT_DATE_ISO;
        if( tz == null ) tz = TimeZone.getDefault();
        SimpleDateFormat f = new SimpleDateFormat(format);
        f.setTimeZone(tz);
        return f.format(date);
    }

    public static String toISOString(Date date)
    { return toISOString(date,FORMAT_DATE_ISO, TimeZone.getTimeZone("UTC")); }

    public static File createDir(String path){
        File file=new File(path);
        if (!file.exists()){
            if (!file.mkdirs()){
                return null;
            }
        }
        return file;
    }

    public static File createFile(String filePath){
        if (!TextUtils.isEmpty(filePath)){
            File file=new File(filePath);
            if (!file.exists()){
                try {
                    if (file.createNewFile()){
                        return file;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }else{
                return file;
            }
        }
        return null;
    }

    public static void saveLog(String string){
        FileOutputStream fos=null;
        String filePath = "/mnt/sdcard/BGReader/BGReader.txt";
        createDir("/mnt/sdcard/BGReader");
        createFile(filePath);
        try {
            fos= new FileOutputStream(filePath, true);
            byte[] bytes=string.getBytes();
            fos.write(bytes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if (fos!=null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
