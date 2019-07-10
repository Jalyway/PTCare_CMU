package com.example.ptcare_cmu;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Cartesian;
import com.anychart.core.cartesian.series.Line;
import com.anychart.data.Mapping;
import com.anychart.data.Set;
import com.anychart.enums.Anchor;
import com.anychart.enums.MarkerType;
import com.anychart.enums.TooltipPositionMode;
import com.anychart.graphics.vector.Stroke;

import java.util.ArrayList;
import java.util.List;

public class LineChartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart_common);

        AnyChartView anyChartView = findViewById(R.id.any_chart_view);
        anyChartView.setProgressBar(findViewById(R.id.progress_bar));

        Cartesian cartesian = AnyChart.line();

        cartesian.animation(true);

        cartesian.padding(10d, 20d, 5d, 20d);

        cartesian.crosshair().enabled(true);
        cartesian.crosshair()
                .yLabel(true)
                // TODO ystroke
                .yStroke((Stroke) null, null, null, (String) null, (String) null);

        cartesian.tooltip().positionMode(TooltipPositionMode.POINT);

        cartesian.title("動作準則分辨結果");

        cartesian.yAxis(0).title("動作準則代碼");
        cartesian.yScale().ticks().interval(1);

        cartesian.xAxis(0).labels().padding(5d, 5d, 5d, 5d);

        List<DataEntry> seriesData = new ArrayList<>();
/*
        for(int i=0;i<Download.result.size();i++)
            seriesData.add(new CustomDataEntry(""+(i+1), Integer.parseInt(Download.result.get(i)), 0, 0));
            */

        seriesData.add(new CustomDataEntry("1", 0, 0, 0));
        seriesData.add(new CustomDataEntry("2", 0, 0, 0));
        seriesData.add(new CustomDataEntry("3", 0, 0, 0));
        seriesData.add(new CustomDataEntry("4", 1, 0, 0));
        seriesData.add(new CustomDataEntry("5", 1, 0, 0));
        seriesData.add(new CustomDataEntry("6", 1, 0, 0));
        seriesData.add(new CustomDataEntry("7", 2, 0, 0));
        seriesData.add(new CustomDataEntry("8", 2, 0, 0));
        seriesData.add(new CustomDataEntry("9", 2, 0, 0));
        seriesData.add(new CustomDataEntry("10", 3, 0, 0));
        seriesData.add(new CustomDataEntry("11", 3, 0, 0));
        seriesData.add(new CustomDataEntry("12", 3, 0, 0));
        seriesData.add(new CustomDataEntry("13", 0, 0, 0));
        seriesData.add(new CustomDataEntry("14", 0, 0, 0));
        seriesData.add(new CustomDataEntry("15", 0, 0, 0));
        seriesData.add(new CustomDataEntry("16", 1, 0, 0));
        seriesData.add(new CustomDataEntry("17", 1, 0, 0));
        seriesData.add(new CustomDataEntry("18", 1, 0, 0));
        seriesData.add(new CustomDataEntry("19", 2, 0, 0));
        seriesData.add(new CustomDataEntry("20", 2, 0, 0));
        seriesData.add(new CustomDataEntry("21", 2, 0, 0));
        seriesData.add(new CustomDataEntry("22", 3, 0, 0));
        seriesData.add(new CustomDataEntry("23", 3, 0, 0));
        seriesData.add(new CustomDataEntry("24", 3, 0, 0));
        seriesData.add(new CustomDataEntry("25", 0, 0, 0));
        seriesData.add(new CustomDataEntry("26", 0, 0, 0));
        seriesData.add(new CustomDataEntry("27", 0, 0, 0));
        seriesData.add(new CustomDataEntry("28", 1, 0, 0));
        seriesData.add(new CustomDataEntry("29", 1, 0, 0));
        seriesData.add(new CustomDataEntry("30", 1, 0, 0));
        seriesData.add(new CustomDataEntry("31", 2, 0, 0));
        seriesData.add(new CustomDataEntry("32", 2, 0, 0));
        seriesData.add(new CustomDataEntry("33", 2, 0, 0));
        seriesData.add(new CustomDataEntry("34", 3, 0, 0));
        seriesData.add(new CustomDataEntry("35", 3, 0, 0));
        seriesData.add(new CustomDataEntry("36", 3, 0, 0));
        seriesData.add(new CustomDataEntry("37", 0, 0, 0));
        seriesData.add(new CustomDataEntry("38", 0, 0, 0));
        seriesData.add(new CustomDataEntry("39", 0, 0, 0));
        seriesData.add(new CustomDataEntry("40", 1, 0, 0));
        seriesData.add(new CustomDataEntry("41", 1, 0, 0));
        seriesData.add(new CustomDataEntry("42", 1, 0, 0));
        seriesData.add(new CustomDataEntry("43", 2, 0, 0));
        seriesData.add(new CustomDataEntry("44", 2, 0, 0));
        seriesData.add(new CustomDataEntry("45", 2, 0, 0));
        seriesData.add(new CustomDataEntry("46", 3, 0, 0));
        seriesData.add(new CustomDataEntry("47", 3, 0, 0));
        seriesData.add(new CustomDataEntry("48", 3, 0, 0));
        seriesData.add(new CustomDataEntry("49", 0, 0, 0));
        seriesData.add(new CustomDataEntry("50", 0, 0, 0));
        seriesData.add(new CustomDataEntry("51", 0, 0, 0));
        seriesData.add(new CustomDataEntry("52", 1, 0, 0));
        seriesData.add(new CustomDataEntry("53", 1, 0, 0));
        seriesData.add(new CustomDataEntry("54", 1, 0, 0));
        seriesData.add(new CustomDataEntry("56", 2, 0, 0));
        seriesData.add(new CustomDataEntry("57", 2, 0, 0));
        seriesData.add(new CustomDataEntry("58", 3, 0, 0));
        seriesData.add(new CustomDataEntry("59", 3, 0, 0));
        seriesData.add(new CustomDataEntry("60", 3, 0, 0));

        Set set = Set.instantiate();
        set.data(seriesData);
        Mapping series1Mapping = set.mapAs("{ x: 'x', value: 'value' }");
        //Mapping series2Mapping = set.mapAs("{ x: 'x', value: 'value2' }");
        //Mapping series3Mapping = set.mapAs("{ x: 'x', value: 'value3' }");

        Line series1 = cartesian.line(series1Mapping);
        series1.name("動作分辨");
        series1.hovered().markers().enabled(true);
        series1.hovered().markers()
                .type(MarkerType.CIRCLE)
                .size(4d);
        series1.tooltip()
                .position("right")
                .anchor(Anchor.LEFT_CENTER)
                .offsetX(5d)
                .offsetY(5d);

        /*Line series2 = cartesian.line(series2Mapping);
        series2.name("Whiskey");
        series2.hovered().markers().enabled(true);
        series2.hovered().markers()
                .type(MarkerType.CIRCLE)
                .size(4d);
        series2.tooltip()
                .position("right")
                .anchor(Anchor.LEFT_CENTER)
                .offsetX(5d)
                .offsetY(5d);
*//*
        Line series3 = cartesian.line(series3Mapping);
        series3.name("Tequila");
        series3.hovered().markers().enabled(true);
        series3.hovered().markers()
                .type(MarkerType.CIRCLE)
                .size(4d);
        series3.tooltip()
                .position("right")
                .anchor(Anchor.LEFT_CENTER)
                .offsetX(5d)
                .offsetY(5d);
*/
        cartesian.legend().enabled(true);
        cartesian.legend().fontSize(13d);
        cartesian.legend().padding(0d, 0d, 10d, 0d);

        anyChartView.setChart(cartesian);
    }

    private class CustomDataEntry extends ValueDataEntry {

        CustomDataEntry(String x, Number value, Number value2, Number value3) {
            super(x, value);
            setValue("value2", value2);
            setValue("value3", value3);
        }

    }

}
