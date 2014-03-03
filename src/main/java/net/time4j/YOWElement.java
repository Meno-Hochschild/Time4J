/*
 * -----------------------------------------------------------------------
 * Copyright © 2012 Meno Hochschild, <http://www.menodata.de/>
 * -----------------------------------------------------------------------
 * This file (YOWElement.java) is part of project Time4J.
 *
 * Time4J is free software: You can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Time4J is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Time4J. If not, see <http://www.gnu.org/licenses/>.
 * -----------------------------------------------------------------------
 */

package net.time4j;

import net.time4j.base.GregorianMath;
import net.time4j.base.MathUtils;
import net.time4j.engine.ChronoElement;
import net.time4j.engine.ChronoEntity;
import net.time4j.engine.ChronoOperator;
import net.time4j.engine.Chronology;
import net.time4j.engine.ElementRule;
import net.time4j.engine.EpochDays;
import net.time4j.engine.UnitRule;
import net.time4j.format.NumericalElement;
import net.time4j.tz.TZID;
import net.time4j.tz.TransitionStrategy;

import java.io.ObjectStreamException;

import static net.time4j.PlainDate.CALENDAR_DATE;
import static net.time4j.PlainTime.WALL_TIME;


/**
 * <p>Repr&auml;sentiert das Jahr in einem ISO-Wochendatum. </p>
 *
 * @author      Meno Hochschild
 * @concurrency <immutable>
 */
final class YOWElement
    extends AbstractValueElement<Integer, PlainDate>
    implements AdjustableElement<Integer, PlainDate>,
               NumericalElement<Integer> {

    //~ Statische Felder/Initialisierungen --------------------------------

    @SuppressWarnings("rawtypes")
    private static final UnitRule U_RULE = new URule();

    /**
     * <p>Singleton-Instanz. </p>
     */
    static final YOWElement INSTANCE = new YOWElement("YEAR_OF_WEEKDATE");

    private static final long serialVersionUID = -6907291758376370420L;

    //~ Instanzvariablen --------------------------------------------------

    private transient final ZonalOperator<PlainDate> previousAdjuster;
    private transient final ZonalOperator<PlainDate> nextAdjuster;

    //~ Konstruktoren -----------------------------------------------------

    private YOWElement(String name) {
        super(name);

        this.previousAdjuster = new YOWRollingAdjuster<PlainDate>(-1);
        this.nextAdjuster = new YOWRollingAdjuster<PlainDate>(1);

    }

    //~ Methoden ----------------------------------------------------------

    @Override
    public Class<Integer> getType() {

        return Integer.class;

    }

    @Override
    public char getSymbol() {

        return 'Y';

    }

    @Override
    public int numerical(Integer value) {

        return value.intValue();

    }

    @Override
    public Integer getDefaultMinimum() {

        return PlainDate.MIN_YEAR;

    }

    @Override
    public Integer getDefaultMaximum() {

        return PlainDate.MAX_YEAR;

    }

    @Override
    public boolean isDateElement() {

        return true;

    }

    @Override
    public boolean isTimeElement() {

        return false;

    }

    @Override
    public ZonalOperator<PlainDate> decremented() {

        return this.previousAdjuster;

    }

    @Override
    public ZonalOperator<PlainDate> incremented() {

        return this.nextAdjuster;

    }

    /**
     * <p>Erzeugt eine Elementregel. </p>
     *
     * @param   <T> either {@code PlainDate} or {@code PlainTimestamp}
     * @param   type    helps to improve type inference
     * @return  new element rule
     */
    static <T extends ChronoEntity<T>> ElementRule<T, Integer> elementRule(
        Class<T> type
    ) {

        return new ERule<T>();

    }

    @SuppressWarnings("unchecked")
    private static <T extends ChronoEntity<T>> UnitRule<T> unitRule() {

        return U_RULE;

    }

    // Ermittelt den Wochenbeginn der ersten Kalenderwoche eines
    // Jahres auf einer day-of-year-Skala (kann auch <= 0 sein).
    private static int getFirstCalendarWeekAsDayOfYear(
        PlainDate date,
        int shift // -1 = Vorjahr, 0 = aktuelles Jahr, +1 = Folgejahr
    ) {

        return getFirstCalendarWeekAsDayOfYear(date.getYear() + shift);

    }

    // Ermittelt den Wochenbeginn der ersten Kalenderwoche eines
    // Jahres auf einer day-of-year-Skala (kann auch <= 0 sein).
    private static int getFirstCalendarWeekAsDayOfYear(int year) {

        Weekday wdNewYear =
            Weekday.valueOf(GregorianMath.getDayOfWeek(year, 1, 1));
        int dow = wdNewYear.getValue(Weekmodel.ISO);

        return (
            (dow <= 8 - Weekmodel.ISO.getMinimalDaysInFirstWeek())
            ? 2 - dow
            : 9 - dow
        );

    }

    // Länge eines Jahres in Tagen
    private static int getLengthOfYear(
        PlainDate date,
        int shift // -1 = Vorjahr, 0 = aktuelles Jahr, +1 = Folgejahr
    ) {

        return (
            GregorianMath.isLeapYear(date.getYear() + shift)
            ? 366
            : 365
        );

    }

    // Optimierung als Ersatz für date.getWeekOfYear()
    private static int getWeekOfYear(PlainDate date) {

        int dayOfYear = date.getDayOfYear();
        int wCurrent = getFirstCalendarWeekAsDayOfYear(date, 0);

        if (wCurrent <= dayOfYear) {
            int result = ((dayOfYear - wCurrent) / 7) + 1;

            if (result >= 53) { // Optimierung
                int wNext =
                    getFirstCalendarWeekAsDayOfYear(date, 1)
                    + getLengthOfYear(date, 0);
                if (wNext <= dayOfYear) {
                    result = 1;
                }
            }

            return result;
        } else {
            int wPrevious = getFirstCalendarWeekAsDayOfYear(date, -1);
            int dayCurrent = dayOfYear + getLengthOfYear(date, -1);
            return ((dayCurrent - wPrevious) / 7) + 1;
        }

    }

    private Object readResolve() throws ObjectStreamException {

        return INSTANCE;

    }

    //~ Innere Klassen ----------------------------------------------------

    /**
     * <p>Spezial-Zeiteinheit f&uuml;r wochenbasierte Jahre. </p>
     */
    static enum YOWUnit
        implements IsoDateUnit, UnitRule.Source {

        //~ Statische Felder/Initialisierungen ----------------------------

        WEEK_BASED_YEARS;

        //~ Methoden ------------------------------------------------------

        @Override
        public char getSymbol() {

            return '\u0000';

        }

        @Override
        public double getLength() {

            return CalendarUnit.YEARS.getLength();

        }

        @Override
        public boolean isCalendrical() {

            return true;

        }

        @Override
        public <T extends ChronoEntity<T>> UnitRule<T> derive(
            Chronology<T> chronology
        ) {

            if (chronology.isRegistered(CALENDAR_DATE)) {
                return YOWElement.unitRule();
            }

            return null;

        }

    }

    private static class YOWRollingAdjuster<T extends ChronoEntity<T>>
        implements ZonalOperator<T> {

        //~ Instanzvariablen ----------------------------------------------

        private final long amount;

        //~ Konstruktoren -------------------------------------------------

        private YOWRollingAdjuster(long amount) {
            super();

            this.amount = amount;

        }

        //~ Methoden ------------------------------------------------------

        @Override
        public T apply(T entity) {

            UnitRule<T> rule = YOWElement.unitRule();
            return rule.addTo(entity, this.amount);

        }

        @Override
        public ChronoOperator<Moment> inStdTimezone() {

            return new Moment.Operator(
                this.onTimestamp(), YOWElement.INSTANCE, OperatorType.YOW);

        }

        @Override
        public ChronoOperator<Moment> inTimezone(
            TZID tzid,
            TransitionStrategy strategy
        ) {

            return new Moment.Operator(
                this.onTimestamp(),
                tzid,
                strategy,
                YOWElement.INSTANCE,
                OperatorType.YOW
            );

        }

        @Override
        public ChronoOperator<PlainTimestamp> onTimestamp() {

            return new YOWRollingAdjuster<PlainTimestamp>(this.amount);

        }

    }

    private static class URule<T extends ChronoEntity<T>>
        implements UnitRule<T> {

        //~ Methoden ------------------------------------------------------

        @Override
        public T addTo(
            T entity,
            long amount
        ) {

            if (amount == 0) {
                return entity;
            }

            int yow =
                MathUtils.safeCast(
                    MathUtils.safeAdd(
                        entity.get(YOWElement.INSTANCE).intValue(),
                        amount)
                    );

            PlainDate date = entity.get(CALENDAR_DATE);
            int woy = date.getWeekOfYear();
            Weekday dow = date.getDayOfWeek();

            if (woy == 53) {
                PlainDate test = PlainDate.of(yow, 26, dow);
                woy = test.getMaximum(Weekmodel.ISO.weekOfYear());
            }

            return entity.with(
                CALENDAR_DATE,
                PlainDate.of(yow, woy, dow));

        }

        @Override
        public long between(
            T start,
            T end
        ) {

            PlainDate startDate = start.get(CALENDAR_DATE);
            PlainDate endDate = end.get(CALENDAR_DATE);

            int startYOW = startDate.get(YOWElement.INSTANCE).intValue();
            int endYOW = endDate.get(YOWElement.INSTANCE).intValue();
            long delta = endYOW - startYOW;

            if (delta != 0) {
                int startWOY = getWeekOfYear(startDate);
                int endWOY = getWeekOfYear(endDate);

                if ((delta > 0) && (startWOY > endWOY)) {
                    delta--;
                } else if ((delta < 0) && (startWOY < endWOY)) {
                    delta++;
                }

                if ((delta != 0) && (startWOY == endWOY)) {
                    int startDOW = startDate.getDayOfWeek().getValue();
                    int endDOW = endDate.getDayOfWeek().getValue();

                    if ((delta > 0) && (startDOW > endDOW)) {
                        delta--;
                    } else if ((delta < 0) && (startDOW < endDOW)) {
                        delta++;
                    }

                    if (
                        (delta != 0)
                        && (startDOW == endDOW)
                        && start.contains(WALL_TIME)
                        && end.contains(WALL_TIME)
                    ) {
                        PlainTime t1 = start.get(WALL_TIME);
                        PlainTime t2 = end.get(WALL_TIME);

                        if ((delta > 0) && t1.isAfter(t2)) {
                            delta--;
                        } else if ((delta < 0) && t1.isBefore(t2)) {
                            delta++;
                        }
                    }
                }
            }

            return delta;

        }

    }

    private static class ERule<T extends ChronoEntity<T>>
        implements ElementRule<T, Integer> {

        //~ Methoden ------------------------------------------------------

        @Override
        public Integer getValue(T context) {

            PlainDate date = context.get(CALENDAR_DATE);
            int year = date.getYear();
            int dayOfYear = date.getDayOfYear();
            int wCurrent = getFirstCalendarWeekAsDayOfYear(date, 0);

            if (wCurrent <= dayOfYear) {
                if (((dayOfYear - wCurrent) / 7) + 1 >= 53) {
                    int wNext =
                        getFirstCalendarWeekAsDayOfYear(date, 1)
                        + getLengthOfYear(date, 0);
                    if (wNext <= dayOfYear) {
                        year++;
                    }
                }
            } else {
                year--;
            }

            return Integer.valueOf(year);

        }

        @Override
        public Integer getMinimum(T context) {

            return YOWElement.INSTANCE.getDefaultMinimum();

        }

        @Override
        public Integer getMaximum(T context) {

            return YOWElement.INSTANCE.getDefaultMaximum();

        }

        @Override
        public boolean isValid(
            T context,
            Integer value
        ) {

            int v = value.intValue();

            return (
                (v >= GregorianMath.MIN_YEAR)
                && (v <= GregorianMath.MAX_YEAR)
            );

        }

        @Override
        public T withValue(
            T context,
            Integer value,
            boolean lenient
        ) {

            PlainDate date = context.get(CALENDAR_DATE);
            date = setYearOfWeekdate(date, value.intValue());
            return context.with(CALENDAR_DATE, date);

        }

        @Override
        public ChronoElement<?> getChildAtFloor(T context) {

            return this.getChild();

        }

        @Override
        public ChronoElement<?> getChildAtCeiling(T context) {

            return this.getChild();

        }

        private ChronoElement<?> getChild() {

            return Weekmodel.ISO.weekOfYear();

        }

        private static PlainDate setYearOfWeekdate(
            PlainDate date,
            int value
        ) {

            int dowCurrent = getFirstCalendarWeekAsDayOfYear(value);
            int weekOfYear = getWeekOfYear(date);
            int dayOfWeek = date.getDayOfWeek().getValue(Weekmodel.ISO);

            long unixDays =
                EpochDays.UNIX.transform(
                    GregorianMath.toMJD(value, 1, 1),
                    EpochDays.MODIFIED_JULIAN_DATE)
                + (dowCurrent - 1)
                + (weekOfYear - 1) * 7
                + (dayOfWeek - 1);

            if (weekOfYear == 53) {
                int wNext =
                    getFirstCalendarWeekAsDayOfYear(value + 1)
                    + (GregorianMath.isLeapYear(value) ? 366 : 365);
                if (((wNext - dowCurrent) / 7) < 53) {
                    unixDays -= 7; // weekOfYear = 52
                }
            }

            return date.withDaysSinceUTC(unixDays - 2 * 365);

        }

    }

}
