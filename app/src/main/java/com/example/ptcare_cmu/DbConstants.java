package com.example.ptcare_cmu;

/*
 * Created by js-mis on 2017/9/15.
 */

import android.provider.BaseColumns;

public interface DbConstants extends BaseColumns
{
    public static final String TABLE_NAME = "motion_guide";

    public static final String Col1 = "motion_name";
    public static final String Col2 = "motion_description";
    public static final String Col3 = "motion_code";
    public static final String Col4 = "mtype_id";
    public static final String Col5 = "_id";
}
