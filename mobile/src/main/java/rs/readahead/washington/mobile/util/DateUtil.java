package rs.readahead.washington.mobile.util;

import android.support.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class DateUtil {
    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("d MMM yyyy", Locale.ROOT);
    private static Calendar mCurrentCalendar;

    public static String getStringFromDate(@NonNull Date date) {
        return dateFormatter.format(date);
    }

    public static Date getCurrentDate(){
        return Calendar.getInstance().getTime();
    }

    public static Date getYesterdaysDate() {
        mCurrentCalendar = Calendar.getInstance();
        mCurrentCalendar.add(Calendar.DATE, -1);
        return mCurrentCalendar.getTime();
    }

    public static boolean isYesterday(Date date) {
        mCurrentCalendar = Calendar.getInstance();
        Calendar reportTime = Calendar.getInstance();
        reportTime.setTime(date);

        mCurrentCalendar.add(Calendar.DATE,-1);

        return mCurrentCalendar.get(Calendar.YEAR) == reportTime.get(Calendar.YEAR)
                && mCurrentCalendar.get(Calendar.MONTH) == reportTime.get(Calendar.MONTH)
                && mCurrentCalendar.get(Calendar.DATE) == reportTime.get(Calendar.DATE);
    }

}
