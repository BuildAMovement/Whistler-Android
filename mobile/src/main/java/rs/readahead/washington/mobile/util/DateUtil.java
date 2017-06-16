package rs.readahead.washington.mobile.util;

import android.content.Context;
import android.content.DialogInterface;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import rs.readahead.washington.mobile.R;

public class DateUtil {
    public static SimpleDateFormat dateFormatter = new SimpleDateFormat("d MMM yyyy");
    public static Calendar mCurrentCalendar;

    public static String getStringFromDate(Date date){
        return dateFormatter.format(date);

    }

    public static Date getCurrentDate(){
        return Calendar.getInstance().getTime();
    }

    public static Date getYesterdaysDate(){
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
