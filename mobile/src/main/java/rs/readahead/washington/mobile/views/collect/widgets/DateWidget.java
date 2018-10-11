/*
 * Copyright (C) 2009 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package rs.readahead.washington.mobile.views.collect.widgets;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;

import org.javarosa.core.model.data.DateData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryPrompt;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;

import java.util.Date;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.util.Util;


/**
 * Based on ODK DateWidget.
 */
public class DateWidget extends QuestionWidget {
    private DatePickerDialog datePickerDialog;

    private Button dateButton;

    private int year;
    private int month;
    private int dayOfMonth;

    private boolean nullAnswer = false;


    public DateWidget(Context context, FormEntryPrompt prompt) {
        super(context, prompt);

        createDatePickerDialog();

        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        addDateButton(linearLayout);

        if (formEntryPrompt.getAnswerValue() == null) {
            clearAnswer();
        } else {
            DateTime dt = new DateTime(((Date) formEntryPrompt.getAnswerValue().getValue()).getTime());

            year = dt.getYear();
            month = dt.getMonthOfYear();
            dayOfMonth = dt.getDayOfMonth();

            setWidgetDate();
            datePickerDialog.updateDate(dt.getYear(), dt.getMonthOfYear() - 1, dt.getDayOfMonth());
        }

        addAnswerView(linearLayout);
    }

    @Override
    public void clearAnswer() {
        nullAnswer = true;
        dateButton.setText(getResources().getString(R.string.ra_no_date_selected)); // todo: say something smart here..
    }

    @Override
    public IAnswerData getAnswer() {
        clearFocus();

        if (nullAnswer) {
            return null;
        }

        LocalDateTime ldt = new LocalDateTime()
                .withYear(year)
                .withMonthOfYear(month)
                .withDayOfMonth(dayOfMonth)
                .withHourOfDay(0)
                .withMinuteOfHour(0);

        return new DateData(ldt.toDate());
    }

    @Override
    public void setFocus(Context context) {
        Util.hideKeyboard(context, this);
    }

    private void setWidgetDate() {
        nullAnswer = false;
        dateButton.setText(getFormattedDate(getContext(), (Date) getAnswer().getValue()));
    }

    private void createDatePickerDialog() {
        datePickerDialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                DateWidget.this.year = year;
                DateWidget.this.month = monthOfYear + 1;
                DateWidget.this.dayOfMonth = dayOfMonth;

                setWidgetDate();
            }
        }, 1971, 1, 1);
    }

    private void addDateButton(LinearLayout linearLayout) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        inflater.inflate(R.layout.collect_widget_date, linearLayout, true);

        dateButton = (Button) linearLayout.findViewById(R.id.dateWidgetButton);
        dateButton.setId(QuestionWidget.newUniqueId());
        dateButton.setText(getResources().getString(R.string.ra_select_date));
        dateButton.setEnabled(!formEntryPrompt.isReadOnly());

        dateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nullAnswer) {
                    DateTime dt = new DateTime();
                    datePickerDialog.updateDate(dt.getYear(), dt.getMonthOfYear() - 1, dt.getDayOfMonth());
                }

                datePickerDialog.show();
            }
        });
    }

    private String getFormattedDate(Context context, Date date) {
        java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
        return dateFormat.format(date);
    }
}
