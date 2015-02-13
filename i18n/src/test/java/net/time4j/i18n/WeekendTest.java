package net.time4j.i18n;

import net.time4j.PlainDate;
import net.time4j.Weekday;
import net.time4j.Weekmodel;

import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


@RunWith(JUnit4.class)
public class WeekendTest {

    @Test
    public void isWeekendISO() {
        assertThat(
            PlainDate.of(2014, 4, 4).isWeekend(Locale.ROOT),
            is(false));
        assertThat(
            PlainDate.of(2014, 4, 5).isWeekend(Locale.ROOT),
            is(true));
        assertThat(
            PlainDate.of(2014, 4, 6).isWeekend(Locale.ROOT),
            is(true));
        assertThat(
            PlainDate.of(2014, 4, 7).isWeekend(Locale.ROOT),
            is(false));
    }

    @Test
    public void isWeekendUS() {
        assertThat(
            PlainDate.of(2014, 4, 4).isWeekend(Locale.US),
            is(false));
        assertThat(
            PlainDate.of(2014, 4, 5).isWeekend(Locale.US),
            is(true));
        assertThat(
            PlainDate.of(2014, 4, 6).isWeekend(Locale.US),
            is(true));
        assertThat(
            PlainDate.of(2014, 4, 7).isWeekend(Locale.US),
            is(false));
    }

    @Test
    public void isWeekendYemen() {
        Locale yemen = new Locale("ar", "Ye"); // Thursday + Friday
        assertThat(
            PlainDate.of(2014, 4, 2).isWeekend(yemen),
            is(false));
        assertThat(
            PlainDate.of(2014, 4, 3).isWeekend(yemen),
            is(true));
        assertThat(
            PlainDate.of(2014, 4, 4).isWeekend(yemen),
            is(true));
        assertThat(
            PlainDate.of(2014, 4, 5).isWeekend(yemen),
            is(false));
    }

    @Test
    public void modelUS() {
        assertThat(
            Weekmodel.of(Locale.US),
            is(Weekmodel.of(Weekday.SUNDAY, 1)));
        assertThat(
            Weekmodel.of(Locale.US).getFirstDayOfWeek(),
            is(Weekday.SUNDAY));
        assertThat(
            Weekmodel.of(Locale.US).getMinimalDaysInFirstWeek(),
            is(1));
        assertThat(
            Weekmodel.of(Locale.US).getStartOfWeekend(),
            is(Weekday.SATURDAY));
        assertThat(
            Weekmodel.of(Locale.US).getEndOfWeekend(),
            is(Weekday.SUNDAY));
        assertThat(
            Weekmodel.of(Locale.US).getFirstWorkday(),
            is(Weekday.MONDAY));
    }

    @Test
    public void modelYemen() {
        Locale yemen = new Locale("ar", "YE");
        assertThat(
            Weekmodel.of(yemen),
            is(Weekmodel.of(
                Weekday.SATURDAY, 1, Weekday.THURSDAY, Weekday.FRIDAY)));
        assertThat(
            Weekmodel.of(yemen).getFirstDayOfWeek(),
            is(Weekday.SATURDAY));
        assertThat(
            Weekmodel.of(yemen).getMinimalDaysInFirstWeek(),
            is(1));
        assertThat(
            Weekmodel.of(yemen).getStartOfWeekend(),
            is(Weekday.THURSDAY));
        assertThat(
            Weekmodel.of(yemen).getEndOfWeekend(),
            is(Weekday.FRIDAY));
        assertThat(
            Weekmodel.of(yemen).getFirstWorkday(),
            is(Weekday.SATURDAY));
    }

    @Test
    public void modelIndia() {
        Locale india = new Locale("", "IN");
        assertThat(
            Weekmodel.of(india),
            is(Weekmodel.of(
                Weekday.SUNDAY, 1, Weekday.SUNDAY, Weekday.SUNDAY)));
        assertThat(
            Weekmodel.of(india).getFirstDayOfWeek(),
            is(Weekday.SUNDAY));
        assertThat(
            Weekmodel.of(india).getMinimalDaysInFirstWeek(),
            is(1));
        assertThat(
            Weekmodel.of(india).getStartOfWeekend(),
            is(Weekday.SUNDAY));
        assertThat(
            Weekmodel.of(india).getEndOfWeekend(),
            is(Weekday.SUNDAY));
        assertThat(
            Weekmodel.of(india).getFirstWorkday(),
            is(Weekday.MONDAY));
    }

    @Test
    public void weekend() {
        Locale yemen = new Locale("ar", "YE");
        PlainDate date = PlainDate.of(2013, 3, 30); // Samstag
        assertThat(date.matches(Weekmodel.ISO.weekend()), is(true));
        assertThat(date.matches(Weekmodel.of(yemen).weekend()), is(false));

        date = PlainDate.of(2013, 3, 28); // Donnerstag
        assertThat(date.matches(Weekmodel.ISO.weekend()), is(false));
        assertThat(date.matches(Weekmodel.of(yemen).weekend()), is(true));
    }

}