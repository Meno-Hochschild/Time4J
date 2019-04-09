/*
 * -----------------------------------------------------------------------
 * Copyright © 2013-2019 Meno Hochschild, <http://www.menodata.de/>
 * -----------------------------------------------------------------------
 * This file (BadiCalendar.java) is part of project Time4J.
 *
 * Time4J is free software: You can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * Time4J is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Time4J. If not, see <http://www.gnu.org/licenses/>.
 * -----------------------------------------------------------------------
 */

package net.time4j.calendar.bahai;

import net.time4j.GeneralTimestamp;
import net.time4j.Moment;
import net.time4j.PlainDate;
import net.time4j.PlainTime;
import net.time4j.SystemClock;
import net.time4j.Weekday;
import net.time4j.Weekmodel;
import net.time4j.base.MathUtils;
import net.time4j.base.TimeSource;
import net.time4j.calendar.StdCalendarElement;
import net.time4j.calendar.astro.SolarTime;
import net.time4j.calendar.astro.StdSolarCalculator;
import net.time4j.calendar.service.StdEnumDateElement;
import net.time4j.calendar.service.StdIntegerDateElement;
import net.time4j.calendar.service.StdWeekdayElement;
import net.time4j.engine.AttributeKey;
import net.time4j.engine.AttributeQuery;
import net.time4j.engine.BasicElement;
import net.time4j.engine.CalendarDays;
import net.time4j.engine.CalendarEra;
import net.time4j.engine.CalendarSystem;
import net.time4j.engine.Calendrical;
import net.time4j.engine.ChronoDisplay;
import net.time4j.engine.ChronoElement;
import net.time4j.engine.ChronoEntity;
import net.time4j.engine.ChronoException;
import net.time4j.engine.ChronoMerger;
import net.time4j.engine.ChronoUnit;
import net.time4j.engine.Chronology;
import net.time4j.engine.ElementRule;
import net.time4j.engine.FormattableElement;
import net.time4j.engine.IntElementRule;
import net.time4j.engine.StartOfDay;
import net.time4j.engine.TimeAxis;
import net.time4j.engine.UnitRule;
import net.time4j.engine.ValidationElement;
import net.time4j.format.Attributes;
import net.time4j.format.CalendarText;
import net.time4j.format.CalendarType;
import net.time4j.format.DisplayElement;
import net.time4j.format.Leniency;
import net.time4j.format.OutputContext;
import net.time4j.format.TextAccessor;
import net.time4j.format.TextElement;
import net.time4j.format.TextWidth;
import net.time4j.tz.OffsetSign;
import net.time4j.tz.TZID;
import net.time4j.tz.Timezone;
import net.time4j.tz.ZonalOffset;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.text.ParsePosition;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


/**
 * <p>Represents the calendar used by the Baha'i community. </p>
 *
 * <p>The calendar composes 19 days to a month, 19 months to a year (plus an intercalary period called Ayyam-i-Ha
 * between the 18th and the 19th month), 19 years to a vahid cycle and finally 19 vahids to a major cycle. Days
 * start at sunset. And a week starts on Friday at sunset. The first year of the calendar begins on 1844-03-21.
 * Years before 2015 start in this implementation always on 21st of March, but then on the day (from sunset
 * to sunset in Teheran) which contains the vernal equinox. The latter change follows a decision made by
 * the <a href="https://en.wikipedia.org/wiki/Universal_House_of_Justice">Universal House of Justice</a>. </p>
 *
 * <p>Following elements which are declared as constants are registered by
 * this class: </p>
 *
 * <ul>
 *  <li>{@link #DAY_OF_DIVISION}</li>
 *  <li>{@link #DAY_OF_WEEK}</li>
 *  <li>{@link #DAY_OF_YEAR}</li>
 *  <li>{@link #AYYAM_I_HA}</li>
 *  <li>{@link #MONTH_OF_YEAR}</li>
 *  <li>{@link #YEAR_OF_VAHID}</li>
 *  <li>{@link #VAHID}</li>
 *  <li>{@link #KULL_I_SHAI}</li>
 *  <li>{@link #YEAR_OF_ERA}</li>
 *  <li>{@link #ERA}</li>
 * </ul>
 *
 * <p>Furthermore, all elements defined in {@code EpochDays} are supported. </p>
 *
 * <p><strong>Formatting and parsing:</strong> When using format patterns the
 * {@link net.time4j.format.expert.PatternType#DYNAMIC dynamic pattern type}
 * is strongly recommended instead of CLDR-like pattern types because this calendar
 * is structurally different from month-based calendars. Following symbol-element
 * table holds: </p>
 *
 * <div style="margin-top:5px;">
 * <table border="1">
 * <caption>Mapping of dynamic pattern symbols</caption>
 * <tr>
 *  <th>element</th><th>symbol</th><th>type</th>
 * </tr>
 * <tr>
 *  <td>ERA</td><td>G</td><td>text</td>
 * </tr>
 * <tr>
 *  <td>KULL_I_SHAI</td><td>k/K</td><td>number</td>
 * </tr>
 * <tr>
 *  <td>VAHID</td><td>v/V</td><td>number</td>
 * </tr>
 * <tr>
 *  <td>YEAR_OF_VAHID</td><td>y/Y</td><td>number/text</td>
 * </tr>
 * <tr>
 *  <td>MONTH_OF_YEAR</td><td>m/M</td><td>number/text</td>
 * </tr>
 * <tr>
 *  <td>AYYAM_I_HA</td><td>A</td><td>text</td>
 * </tr>
 * <tr>
 *  <td>DAY_OF_DIVISION</td><td>d/D</td><td>number</td>
 * </tr>
 * <tr>
 *  <td>DAY_OF_WEEK</td><td>E</td><td>text</td>
 * </tr>
 * </table>
 * </div>
 *
 * <p>It is strongly recommended to use the or-operator &quot;|&quot; in format patterns because not every date of
 * this calendar has a month. Example: </p>
 *
 * <pre>
 *     ChronoFormatter&lt;BadiCalendar&gt; f =
 *      ChronoFormatter.ofPattern(
 *          &quot;k.v.y.m.d|k.v.y.A.d&quot;,
 *          PatternType.DYNAMIC,
 *          Locale.GERMAN,
 *          BadiCalendar.axis());
 *     assertThat(
 *      f.print(BadiCalendar.of(5, 11, BadiMonth.JALAL, 13)),
 *      is(&quot;1.5.11.2.13&quot;));
 *     assertThat(
 *      f.print(BadiCalendar.ofIntercalary(5, 11, 2)),
 *      is(&quot;1.5.11.Aiyam-e Ha'.2&quot;));
 * </pre>
 *
 * @author  Meno Hochschild
 * @since   5.3
 * @doctags.concurrency {immutable}
 */
/*[deutsch]
 * <p>Repr&auml;sentiert den von der Baha'i-Gemeinde verwendeten Kalender. </p>
 *
 * <p>Der Kalender setzt sich aus 19 Tagen je Monat, dann aus 19 Monaten je Jahr (zuz&uuml;glich einer Periode
 * von 4 oder 5 eingeschobenen Tagen nach dem achtzehnten Monat, genannt Ayyam-i-Ha), dann aus 19 Jahren je
 * Einheitszyklus (Vahid) und schließlich aus 19 Einheitszyklen je Hauptzyklus (kull-i-shai) zusammen. Die
 * Tage fangen zum Sonnenuntergang des vorherigen Tages an, so da&szlig; die Woche am Freitagabend beginnt.
 * Das erste Jahr des Kalenders beginnt zum Datum 1844-03-21. Jahre vor 2015 fangen immer am 21. M&aumlr;z
 * an, danach an dem Tag (von Sonnenuntergang zu Sonnenuntergang), der den Fr&uuml;hlingspunkt in Teheran
 * enth&auml;lt. Dieser Regelwechsel folgt einer Entscheidung, die vom
 * <a href="https://en.wikipedia.org/wiki/Universal_House_of_Justice">Universal House of Justice</a>
 * getroffen wurde. </p>
 *
 * <p>Folgende als Konstanten deklarierte Elemente werden von dieser Klasse registriert: </p>
 *
 * <ul>
 *  <li>{@link #DAY_OF_DIVISION}</li>
 *  <li>{@link #DAY_OF_WEEK}</li>
 *  <li>{@link #DAY_OF_YEAR}</li>
 *  <li>{@link #AYYAM_I_HA}</li>
 *  <li>{@link #MONTH_OF_YEAR}</li>
 *  <li>{@link #YEAR_OF_VAHID}</li>
 *  <li>{@link #VAHID}</li>
 *  <li>{@link #KULL_I_SHAI}</li>
 *  <li>{@link #YEAR_OF_ERA}</li>
 *  <li>{@link #ERA}</li>
 * </ul>
 *
 * <p>Au&slig;erdem werden alle Elemente von {@code EpochDays} unterst&uuml;tzt. </p>
 *
 * <p><strong>Formatieren und Interpretation:</strong> Wenn Formatmuster verwendet werden,
 * wird der {@link net.time4j.format.expert.PatternType#DYNAMIC dynamische Formatmustertyp}
 * anstelle von CLDR-basierten Formatmustertypen empfohlen, weil dieser Kalender strukturell
 * von monatsbasierten Kalendern verschieden ist. Folgende Symbol-Element-Tabelle gilt: </p>
 *
 * <div style="margin-top:5px;">
 * <table border="1">
 * <caption>Zuordnung von dynamischen Mustersymbolen</caption>
 * <tr>
 *  <th>Element</th><th>Symbol</th><th>Typ</th>
 * </tr>
 * <tr>
 *  <td>ERA</td><td>G</td><td>text</td>
 * </tr>
 * <tr>
 *  <td>KULL_I_SHAI</td><td>k/K</td><td>number</td>
 * </tr>
 * <tr>
 *  <td>VAHID</td><td>v/V</td><td>number</td>
 * </tr>
 * <tr>
 *  <td>YEAR_OF_VAHID</td><td>y/Y</td><td>number/text</td>
 * </tr>
 * <tr>
 *  <td>MONTH_OF_YEAR</td><td>m/M</td><td>number/text</td>
 * </tr>
 * <tr>
 *  <td>AYYAM_I_HA</td><td>A</td><td>text</td>
 * </tr>
 * <tr>
 *  <td>DAY_OF_DIVISION</td><td>d/D</td><td>number</td>
 * </tr>
 * <tr>
 *  <td>DAY_OF_WEEK</td><td>E</td><td>text</td>
 * </tr>
 * </table>
 * </div>
 *
 * <p>Weil nicht jedes Datum dieses Kalenders einen Monat hat, ist es angeraten, mit dem Oder-Operator
 * &quot;|&quot; in der Formatierung zu arbeiten. Beispiel: </p>
 *
 * <pre>
 *     ChronoFormatter&lt;BadiCalendar&gt; f =
 *      ChronoFormatter.ofPattern(
 *          &quot;k.v.y.m.d|k.v.y.A.d&quot;,
 *          PatternType.DYNAMIC,
 *          Locale.GERMAN,
 *          BadiCalendar.axis());
 *     assertThat(
 *      f.print(BadiCalendar.of(5, 11, BadiMonth.JALAL, 13)),
 *      is(&quot;1.5.11.2.13&quot;));
 *     assertThat(
 *      f.print(BadiCalendar.ofIntercalary(5, 11, 2)),
 *      is(&quot;1.5.11.Aiyam-e Ha'.2&quot;));
 * </pre>
 *
 * @author  Meno Hochschild
 * @since   5.3
 * @doctags.concurrency {immutable}
 */
@CalendarType("extra/bahai")
public final class BadiCalendar
    extends Calendrical<BadiCalendar.Unit, BadiCalendar> {

    //~ Statische Felder/Initialisierungen --------------------------------

    /**
     * Format attribute which controls the content of some text elements like months or weekdays.
     *
     * <p>Standard value is: {@link FormattedContent#TRANSCRIPTION}</p>
     */
    /*[deutsch]
     * Formatattribut, das den Inhalt von einigen Textelementen wie Monaten oder Wochentagen steuert.
     *
     * <p>Standardwert ist: {@link FormattedContent#TRANSCRIPTION}</p>
     */
    public static final AttributeKey<FormattedContent> TEXT_CONTENT_ATTRIBUTE =
        Attributes.createKey("FORMATTED_CONTENT", FormattedContent.class);

    private static final SolarTime TEHERAN =
        SolarTime.ofLocation()
            .easternLongitude(51, 25, 0.0)
            .northernLatitude(35, 42, 0.0)
            .usingCalculator(StdSolarCalculator.TIME4J)
            .build();

    private static final int KULL_I_SHAI_INDEX = 0;
    private static final int VAHID_INDEX = 1;
    private static final int YEAR_INDEX = 2;
    private static final int DAY_OF_DIVISION_INDEX = 3;
    private static final int DAY_OF_YEAR_INDEX = 4;
    private static final int YOE_INDEX = 5;

    /**
     * <p>Represents the Bahai era. </p>
     */
    /*[deutsch]
     * <p>Repr&auml;sentiert die Bahai-&Auml;ra. </p>
     */
    @FormattableElement(format = "G", dynamic = true)
    public static final ChronoElement<BadiEra> ERA =
        new StdEnumDateElement<BadiEra, BadiCalendar>("ERA", BadiCalendar.class, BadiEra.class, 'G') {
            @Override
            protected TextAccessor accessor(
                AttributeQuery attributes,
                OutputContext outputContext,
                boolean leap
            ) {
                Locale lang = attributes.get(Attributes.LANGUAGE, Locale.ROOT);
                TextWidth width = attributes.get(Attributes.TEXT_WIDTH, TextWidth.WIDE);
                return BadiEra.accessor(lang, width);
            }
        };

    /**
     * <p>Represents the proleptic year of era (relative to gregorian year 1844). </p>
     *
     * <p>Note that this kind of year definition which counts years since the Bahai era is unusual.
     * For the standard way to count years see the elements {@link #KULL_I_SHAI}, {@link #VAHID}
     * and {@link #YEAR_OF_VAHID}. This element can only be parsed if there is no vahid-related element
     * and if the era is also present. </p>
     */
    /*[deutsch]
     * <p>Repr&auml;sentiert das proleptische Jahr seit der Bahai-&Auml;ra (1844). </p>
     *
     * <p>Achtung: Diese Jahresdefinition ist un&uuml;blich. Der Standardweg verwendet stattdessen die
     * Elemente {@link #KULL_I_SHAI}, {@link #VAHID} und {@link #YEAR_OF_VAHID}. Dieses Element kann
     * nur in Anwesenheit einer &Auml;ra vom Parser interpretiert werden, und auch nur dann, wenn es
     * kein VAHID- oder YEAR_OF_VAHID-Element gibt. </p>
     */
    public static final StdCalendarElement<Integer, BadiCalendar> YEAR_OF_ERA =
        new StdIntegerDateElement<>("YEAR_OF_ERA", BadiCalendar.class, 1, 3 * 361, '\u0000');

    /**
     * <p>Represents the major cycle (kull-i-shai). </p>
     *
     * <p>This calendar supports the values 1-3. However, only the first major cycle can be interpreted as safe
     * while the higher values are an astronomic approximation. </p>
     */
    /*[deutsch]
     * <p>Repr&auml;sentiert den Hauptzyklus (kull-i-shai). </p>
     *
     * <p>Dieser Kalender unterst&uuml;tzt die Werte 1-3. Allerdings kann nur der erste Hauptzyklus als
     * gesichert angesehen werden. Die h&ouml;heren Werte sind lediglich eine astronomische Ann&auml;herung. </p>
     */
    @FormattableElement(format = "K", alt = "k", dynamic = true)
    public static final ChronoElement<Integer> KULL_I_SHAI =
        new StdIntegerDateElement<BadiCalendar>("KULL_I_SHAI", BadiCalendar.class, 1, 3, 'K') {
            @Override
            public String getDisplayName(Locale language) {
                return CalendarText.getInstance("extra/bahai", language).getTextForms().get("K");
            }
        };

    /**
     * <p>Represents the vahid cycle which consists of 19 years. </p>
     */
    /*[deutsch]
     * <p>Repr&auml;sentiert den Vahid-Zyklus, der aus 19 Jahren besteht. </p>
     */
    @FormattableElement(format = "V", alt = "v", dynamic = true)
    public static final StdCalendarElement<Integer, BadiCalendar> VAHID =
        new StdIntegerDateElement<BadiCalendar>("VAHID", BadiCalendar.class, 1, 19, 'V') {
            @Override
            public String getDisplayName(Locale language) {
                return CalendarText.getInstance("extra/bahai", language).getTextForms().get("V");
            }
        };

    /**
     * <p>Represents the year of vahid cycle. </p>
     *
     * <p>The dynamic pattern symbol Y or y will print the year of vahid either as text (big symbol letter)
     * or as number (small symbol letter). </p>
     */
    /*[deutsch]
     * <p>Repr&auml;sentiert das Jahr des assoziierten Vahid-Zyklus. </p>
     *
     * <p>Das dynamische Formatmustersymbol Y oder y wird dieses Jahr entweder als Text (Gro&szlig;buchstabe)
     * oder als Zahl (Kleinbuchstabe) formatieren. </p>
     */
    @FormattableElement(format = "Y", alt = "y", dynamic = true)
    public static final TextElement<Integer> YEAR_OF_VAHID = YOV.SINGLETON;

    /**
     * <p>Represents the month if available. </p>
     *
     * <p><strong>Warning:</strong> A Badi date does not always have a month. If the
     * date is an intercalary day (Ayyam-i-Ha) then any access via {@code get(MONTH_OF_YEAR)}
     * to this element will be rejected by raising an exception. Users have first to make sure
     * that the date is not such an intercalary day. </p>
     *
     * <p>However, it is always possible to query the date for the minimum or maximum month
     * or to set the date to a month-related day even if the actual date is an intercalary day. </p>
     *
     * @see     #isIntercalaryDay()
     * @see     #hasMonth()
     */
    /*[deutsch]
     * <p>Repr&auml;sentiert den Monat, wenn vorhanden. </p>
     *
     * <p><strong>Warnung:</strong> Ein Badi-Datum hat nicht immer einen Monat. Wenn
     * es einen Erg&auml;nzungstag darstellt (Ayyam-i-Ha), dann wird jeder Zugriff per {@code get(MONTH_OF_YEAR)}
     * auf dieses Element mit einer Ausnahme quittiert. Anwender m&uuml;ssen zuerst sicherstellen, da&szlig; das
     * Datum kein solcher Erg&auml;nzungstag ist. </p>
     *
     * <p>Allerdings ist es immer m&ouml;glich, das aktuelle Datum nach dem minimalen oder maximalen
     * Monat zu fragen oder das aktuelle Datum auf einen monatsbezogenen Tag zu setzen,
     * selbst wenn das Datum ein Erg&auml;nzungstag ist. </p>
     *
     * @see     #isIntercalaryDay()
     * @see     #hasMonth()
     */
    @FormattableElement(format = "M", alt = "m", dynamic = true)
    public static final StdCalendarElement<BadiMonth, BadiCalendar> MONTH_OF_YEAR = MonthElement.SINGLETON;

    /**
     * <p>Represents the period of intercalary days if available. </p>
     *
     * <p><strong>Warning:</strong> A Badi date often does not have such a period. If the
     * date is not an intercalary day (Ayyam-i-Ha) then any access via {@code get(AYYAM_I_HA)}
     * to this element will be rejected by raising an exception. Users have first to make sure
     * that the date is such an intercalary day. </p>
     *
     * <p>This element cannot be formatted in a numeric way but only as text. Therefore the dynamic
     * format symbol A is only permitted as big letter. </p>
     *
     * @see     #isIntercalaryDay()
     * @see     #hasMonth()
     */
    /*[deutsch]
     * <p>Repr&auml;sentiert die Erg&auml;nzungstage, wenn vorhanden. </p>
     *
     * <p><strong>Warnung:</strong> Ein Badi-Datum liegt oft nicht auf einem Erg&auml;nzungstag.
     * Wenn es nicht einen Erg&auml;nzungstag darstellt (Ayyam-i-Ha), dann wird jeder Zugriff per
     * {@code get(AYYAM_I_HA)} auf dieses Element mit einer Ausnahme quittiert. Anwender m&uuml;ssen
     * zuerst sicherstellen, da&szlig; das Datum ein solcher Erg&auml;nzungstag ist. </p>
     *
     * <p>Dieses Element kann nicht numerisch formatiert werden, sondern nur als Text. Deshalb ist
     * das dynamische Formatmustersymbol A nur als Gro&szlig;buchstabe zul&auml;ssig. </p>
     *
     * @see     #isIntercalaryDay()
     * @see     #hasMonth()
     */
    @FormattableElement(format = "A", dynamic = true)
    public static final ChronoElement<BadiIntercalaryDays> AYYAM_I_HA = IntercalaryAccess.SINGLETON;

    /**
     * <p>Represents the day of month or an intercalary day. </p>
     */
    /*[deutsch]
     * <p>Repr&auml;sentiert den Tag des Monats oder einen Erg&auml;nzungstag. </p>
     */
    @FormattableElement(format = "D", alt = "d", dynamic = true)
    public static final StdCalendarElement<Integer, BadiCalendar> DAY_OF_DIVISION =
        new StdIntegerDateElement<>("DAY_OF_DIVISION", BadiCalendar.class, 1, 19, 'D');

    /**
     * <p>Represents the day of year. </p>
     */
    /*[deutsch]
     * <p>Repr&auml;sentiert den Tag des Jahres. </p>
     */
    public static final StdCalendarElement<Integer, BadiCalendar> DAY_OF_YEAR =
        new StdIntegerDateElement<>("DAY_OF_YEAR", BadiCalendar.class, 1, 365, '\u0000');

    /**
     * <p>Represents the day of week. </p>
     *
     * <p>If the day-of-week is set to a new value then Time4J handles the calendar week
     * as starting on Saturday. </p>
     */
    /*[deutsch]
     * <p>Repr&auml;sentiert den Tag der Woche. </p>
     *
     * <p>Wenn der Tag der Woche auf einen neuen Wert gesetzt wird, behandelt Time4J die
     * Kalenderwoche so, da&szlig; sie am Samstag beginnt. </p>
     */
    @FormattableElement(format = "E", dynamic = true)
    public static final StdCalendarElement<Weekday, BadiCalendar> DAY_OF_WEEK = DowElement.SINGLETON;

    private static final CalendarSystem<BadiCalendar> CALSYS;
    private static final TimeAxis<BadiCalendar.Unit, BadiCalendar> ENGINE;

    static {
        CALSYS = new Transformer();

        TimeAxis.Builder<BadiCalendar.Unit, BadiCalendar> builder =
            TimeAxis.Builder.setUp(
                BadiCalendar.Unit.class,
                BadiCalendar.class,
                new Merger(),
                CALSYS)
            .appendElement(
                ERA,
                new EraRule())
            .appendElement(
                YEAR_OF_ERA,
                new IntegerRule(YOE_INDEX),
                Unit.YEARS)
            .appendElement(
                KULL_I_SHAI,
                new IntegerRule(KULL_I_SHAI_INDEX))
            .appendElement(
                VAHID,
                new IntegerRule(VAHID_INDEX),
                Unit.VAHID_CYCLES)
            .appendElement(
                YEAR_OF_VAHID,
                new IntegerRule(YEAR_INDEX),
                Unit.YEARS)
            .appendElement(
                MONTH_OF_YEAR,
                new MonthRule(),
                Unit.MONTHS)
            .appendElement(
                AYYAM_I_HA,
                IntercalaryAccess.SINGLETON)
            .appendElement(
                DAY_OF_DIVISION,
                new IntegerRule(DAY_OF_DIVISION_INDEX),
                Unit.DAYS)
            .appendElement(
                DAY_OF_YEAR,
                new IntegerRule(DAY_OF_YEAR_INDEX),
                Unit.DAYS)
            .appendElement(
                DAY_OF_WEEK,
                new WeekdayRule(),
                Unit.DAYS)
            .appendUnit(
                Unit.VAHID_CYCLES,
                new FUnitRule(Unit.VAHID_CYCLES),
                Unit.VAHID_CYCLES.getLength(),
                Collections.singleton(Unit.YEARS))
            .appendUnit(
                Unit.YEARS,
                new FUnitRule(Unit.YEARS),
                Unit.YEARS.getLength(),
                Collections.singleton(Unit.VAHID_CYCLES))
            .appendUnit(
                Unit.MONTHS,
                new FUnitRule(Unit.MONTHS),
                Unit.MONTHS.getLength())
            .appendUnit(
                Unit.WEEKS,
                new FUnitRule(Unit.WEEKS),
                Unit.WEEKS.getLength(),
                Collections.singleton(Unit.DAYS))
            .appendUnit(
                Unit.DAYS,
                new FUnitRule(Unit.DAYS),
                Unit.DAYS.getLength(),
                Collections.singleton(Unit.WEEKS));
        ENGINE = builder.build();
    }

//    private static final long serialVersionUID = -6054794927532842783L;

    //~ Instanzvariablen --------------------------------------------------

    private transient final int major;
    private transient final int cycle;
    private transient final int year;
    private transient final int division;
    private transient final int day;

    //~ Konstruktoren -----------------------------------------------------

    private BadiCalendar(
        int major,
        int cycle,
        int year,
        int division,
        int day
    ) {
        super();

        this.major = major;
        this.cycle = cycle;
        this.year = year;
        this.division = division;
        this.day = day;

    }

    //~ Methoden ----------------------------------------------------------

    /**
     * <p>Creates a new instance of a Badi calendar date. </p>
     *
     * @param   kullishay       major cycle of 361 years (only values 1, 2 or 3 are permitted)
     * @param   vahid           19-year-cycle (in range 1-19)
     * @param   yearOfVahid     year in range 1-19
     * @param   division        either {@code BadiMonth} or {@code BadiIntercalaryDays}
     * @param   day             day in range 1-19 (1-4/5 in case of Ayyam-i-Ha)
     * @return  new instance of {@code BadiCalendar}
     * @throws  IllegalArgumentException in case of any inconsistencies
     */
    /*[deutsch]
     * <p>Erzeugt ein neues Badi-Kalenderdatum. </p>
     *
     * @param   kullishay       major cycle of 361 years (only values 1, 2 or 3 are permitted)
     * @param   vahid           19-year-cycle (in range 1-19)
     * @param   yearOfVahid     year in range 1-19
     * @param   division        either {@code BadiMonth} or {@code BadiIntercalaryDays}
     * @param   day             day in range 1-19 (1-4/5 in case of Ayyam-i-Ha)
     * @return  new instance of {@code BadiCalendar}
     * @throws  IllegalArgumentException in case of any inconsistencies
     */
    public static BadiCalendar ofComplete(
        int kullishay,
        int vahid,
        int yearOfVahid,
        BadiDivision division,
        int day
    ) {

        if ((kullishay < 1) || (kullishay > 3)) {
            throw new IllegalArgumentException("Major cycle (kull-i-shai) out of range 1-3: " + kullishay);
        } else if ((vahid < 1) || (vahid > 19)) {
            throw new IllegalArgumentException("Vahid cycle out of range 1-19: " + vahid);
        } else if ((yearOfVahid < 1) || (yearOfVahid > 19)) {
            throw new IllegalArgumentException("Year out of range 1-19: " + yearOfVahid);
        } else if (division instanceof BadiMonth) {
            if ((day < 1) || (day > 19)) {
                throw new IllegalArgumentException("Day out of range 1-19: " + day);
            } else {
                return new BadiCalendar(kullishay, vahid, yearOfVahid, BadiMonth.class.cast(division).getValue(), day);
            }
        } else if (division == BadiIntercalaryDays.AYYAM_I_HA) {
            int max = isLeapYear(kullishay, vahid, yearOfVahid) ? 5 : 4;
            if ((day < 1) || (day > max)) {
                throw new IllegalArgumentException("Day out of range 1-" + max + ": " + day);
            } else {
                return new BadiCalendar(kullishay, vahid, yearOfVahid, 0, day);
            }
        } else if (division == null) {
            throw new NullPointerException("Missing Badi month or Ayyam-i-Ha.");
        } else {
            throw new IllegalArgumentException("Invalid implementation of Badi division: " + division);
        }

    }

    /**
     * <p>Creates a new instance of a Badi calendar date in first major cycle (gregorian years 1844-2204). </p>
     *
     * @param   vahid           19-year-cycle (in range 1-19)
     * @param   yearOfVahid     year in range 1-19
     * @param   month           Badi month
     * @param   day             day in range 1-19
     * @return  new instance of {@code BadiCalendar}
     * @throws  IllegalArgumentException in case of any inconsistencies
     */
    /*[deutsch]
     * <p>Erzeugt ein neues Badi-Datum im ersten Hauptzyklus (gregorianische Jahre 1844-2204). </p>
     *
     * @param   vahid           19-year-cycle (in range 1-19)
     * @param   yearOfVahid     year in range 1-19
     * @param   month           Badi month
     * @param   day             day in range 1-19
     * @return  new instance of {@code BadiCalendar}
     * @throws  IllegalArgumentException in case of any inconsistencies
     */
    public static BadiCalendar of(
        int vahid,
        int yearOfVahid,
        BadiMonth month,
        int day
    ) {

        return BadiCalendar.ofComplete(1, vahid, yearOfVahid, month, day);

    }

    /**
     * <p>Creates a new instance of a Badi calendar date in first major cycle (gregorian years 1844-2204). </p>
     *
     * @param   vahid           19-year-cycle (in range 1-19)
     * @param   yearOfVahid     year in range 1-19
     * @param   month           month in range 1-19
     * @param   day             day in range 1-19
     * @return  new instance of {@code BadiCalendar}
     * @throws  IllegalArgumentException in case of any inconsistencies
     */
    /*[deutsch]
     * <p>Erzeugt ein neues Badi-Datum im ersten Hauptzyklus (gregorianische Jahre 1844-2204). </p>
     *
     * @param   vahid           19-year-cycle (in range 1-19)
     * @param   yearOfVahid     year in range 1-19
     * @param   month           month in range 1-19
     * @param   day             day in range 1-19
     * @return  new instance of {@code BadiCalendar}
     * @throws  IllegalArgumentException in case of any inconsistencies
     */
    public static BadiCalendar of(
        int vahid,
        int yearOfVahid,
        int month,
        int day
    ) {

        return BadiCalendar.ofComplete(1, vahid, yearOfVahid, BadiMonth.valueOf(month), day);

    }

    /**
     * <p>Creates a new instance of a Badi calendar date in first major cycle (gregorian years 1844-2204). </p>
     *
     * @param   vahid           19-year-cycle (in range 1-19)
     * @param   yearOfVahid     year in range 1-19
     * @param   day             day in range 1-4/5
     * @return  new instance of {@code BadiCalendar} in the Ayyam-i-Ha-period
     * @throws  IllegalArgumentException in case of any inconsistencies
     */
    /*[deutsch]
     * <p>Erzeugt ein neues Badi-Datum im ersten Hauptzyklus (gregorianische Jahre 1844-2204). </p>
     *
     * @param   vahid           19-year-cycle (in range 1-19)
     * @param   yearOfVahid     year in range 1-19
     * @param   day             day in range 1-4/5
     * @return  new instance of {@code BadiCalendar} in the Ayyam-i-Ha-period
     * @throws  IllegalArgumentException in case of any inconsistencies
     */
    public static BadiCalendar ofIntercalary(
        int vahid,
        int yearOfVahid,
        int day
    ) {

        return BadiCalendar.ofComplete(1, vahid, yearOfVahid, BadiIntercalaryDays.AYYAM_I_HA, day);

    }

    /**
     * <p>Obtains the current calendar date in system time. </p>
     *
     * <p>Convenient short-cut for: {@code SystemClock.inLocalView().now(BadiCalendar.axis())}. </p>
     *
     * @return  current calendar date in system time zone using the system clock
     * @see     SystemClock#inLocalView()
     * @see     net.time4j.ZonalClock#now(Chronology)
     */
    /*[deutsch]
     * <p>Ermittelt das aktuelle Kalenderdatum in der Systemzeit. </p>
     *
     * <p>Bequeme Abk&uuml;rzung f&uuml;r:
     * {@code SystemClock.inLocalView().now(BadiCalendar.axis())}. </p>
     *
     * @return  current calendar date in system time zone using the system clock
     * @see     SystemClock#inLocalView()
     * @see     net.time4j.ZonalClock#now(net.time4j.engine.Chronology)
     */
    public static BadiCalendar nowInSystemTime() {

        return SystemClock.inLocalView().now(BadiCalendar.axis());

    }

    /**
     * <p>Yields the major cycle (kull-i-shai) which is 361 years long. </p>
     *
     * @return  int
     * @see     #KULL_I_SHAI
     */
    /*[deutsch]
     * <p>Liefert den Hauptzyklus (kull-i-shai), der 361 Jahre lang ist. </p>
     *
     * @return  int
     * @see     #KULL_I_SHAI
     */
    public int getKullishai() {

        return this.major;

    }

    /**
     * <p>Yields the 19-year-cycle (vahid = unity). </p>
     *
     * @return  int
     * @see     #VAHID
     */
    /*[deutsch]
     * <p>Liefert den 19-Jahre-Zyklus (vahid = Einheit). </p>
     *
     * @return  int
     * @see     #VAHID
     */
    public int getVahid() {

        return this.cycle;

    }

    /**
     * <p>Yields the Badi year related to the vahid cycle. </p>
     *
     * @return  int (1-19)
     * @see     #YEAR_OF_VAHID
     */
    /*[deutsch]
     * <p>Liefert das Badi-Jahr des aktuellen Vahid-Zyklus. </p>
     *
     * @return  int (1-19)
     * @see     #YEAR_OF_VAHID
     */
    public int getYearOfVahid() {

        return this.year;

    }

    /**
     * <p>Yields the Badi month if available. </p>
     *
     * @return  month enum
     * @throws  ChronoException if this date is an intercalary day (Ayyam-i-Ha)
     * @see     #MONTH_OF_YEAR
     * @see     #isIntercalaryDay()
     * @see     #hasMonth()
     */
    /*[deutsch]
     * <p>Liefert den Badi-Monat, wenn vorhanden. </p>
     *
     * @return  month enum
     * @throws  ChronoException if this date is an intercalary day (Ayyam-i-Ha)
     * @see     #MONTH_OF_YEAR
     * @see     #isIntercalaryDay()
     * @see     #hasMonth()
     */
    public BadiMonth getMonth() {

        if (this.division == 0) {
            throw new ChronoException(
                "Intercalary days (Ayyam-i-Ha) do not represent any month: " + this.toString());
        }

        return BadiMonth.valueOf(this.division);

    }

    /**
     * Obtains either the month or the Ayyam-i-Ha-period.
     *
     * @return  BadiDivision
     */
    /*[deutsch]
     * Liefert entweder den Monat oder die Ayyam-i-Ha-Periode.
     *
     * @return  BadiDivision
     */
    public BadiDivision getDivision() {

        return this.isIntercalaryDay() ? BadiIntercalaryDays.AYYAM_I_HA : this.getMonth();

    }

    /**
     * <p>Yields the day of either Badi month or Ayyam-i-Ha. </p>
     *
     * @return  int (1-4/5/19)
     * @see     #DAY_OF_DIVISION
     */
    /*[deutsch]
     * <p>Liefert den Tag des Badi-Monats oder der Ayyam-i-Ha-Periode. </p>
     *
     * @return  int (1-4/5/19)
     * @see     #DAY_OF_DIVISION
     */
    public int getDayOfDivision() {

        return this.day;

    }

    /**
     * <p>Determines the day of standard-week (with seven days). </p>
     *
     * @return  Weekday
     * @see     #DAY_OF_WEEK
     */
    /*[deutsch]
     * <p>Ermittelt den Wochentag bezogen auf eine 7-Tage-Woche. </p>
     *
     * @return  Weekday
     * @see     #DAY_OF_WEEK
     */
    public Weekday getDayOfWeek() {

        long utcDays = CALSYS.transform(this);
        return Weekday.valueOf(MathUtils.floorModulo(utcDays + 5, 7) + 1);

    }

    /**
     * <p>Yields the day of year. </p>
     *
     * @return  int
     * @see     #DAY_OF_YEAR
     */
    /*[deutsch]
     * <p>Liefert den Tag des Jahres. </p>
     *
     * @return  int
     * @see     #DAY_OF_YEAR
     */
    public int getDayOfYear() {

        switch (this.division) {
            case 0:
                return 18 * 19 + this.day;
            case 19:
                return 18 * 19 + (this.isLeapYear() ? 5 : 4) + this.day;
            default:
                return (this.division - 1) * 19 + this.day;
        }

    }

    /**
     * <p>Is this date an intercalary day? </p>
     *
     * <p>A date in the Badi calendar has either a month or is an intercalary day. </p>
     *
     * @return  boolean
     * @see     #hasMonth()
     * @see     BadiIntercalaryDays#AYYAM_I_HA
     */
    /*[deutsch]
     * <p>Liegt dieses Datum auf einem Erg&auml;nzungstag? </p>
     *
     * <p>Ein Datum im Badi-Kalender hat entweder einen Monat
     * oder ist ein Erg&auml;nzungstag (eingeschobener Tag). </p>
     *
     * @return  boolean
     * @see     #hasMonth()
     * @see     BadiIntercalaryDays#AYYAM_I_HA
     */
    public boolean isIntercalaryDay() {

        return (this.division == 0);

    }

    /**
     * <p>Does this date contain a month? </p>
     *
     * <p>A date in the Badi calendar has either a month or is an intercalary day. </p>
     *
     * @return  boolean
     * @see     #isIntercalaryDay()
     */
    /*[deutsch]
     * <p>Liegt dieses Datum in einem Monat? </p>
     *
     * <p>Ein Datum im Badi-Kalender hat entweder einen Monat
     * oder ist ein Erg&auml;nzungstag (eingeschobener Tag). </p>
     *
     * @return  boolean
     * @see     #isIntercalaryDay()
     */
    public boolean hasMonth() {

        return (this.division > 0);

    }

    /**
     * <p>Is the year of this date a leap year? </p>
     *
     * @return  boolean
     */
    /*[deutsch]
     * <p>Liegt dieses Datum in einem Schaltjahr? </p>
     *
     * @return  boolean
     */
    public boolean isLeapYear() {

        return isLeapYear(this.major, this.cycle, this.year);

    }

    /**
     * <p>Is given Badi year a leap year? </p>
     *
     * @param   kullishay       major cycle of 361 years (only values 1, 2 or 3 are permitted)
     * @param   vahid           19-year-cycle (in range 1-19)
     * @param   yearOfVahid     year in range 1-19
     * @return  boolean
     * @throws  IllegalArgumentException in case of any inconsistencies
     */
    /*[deutsch]
     * <p>Ist das angegebene Badi-Jahr ein Schaltjahr? </p>
     *
     * @param   kullishay       major cycle of 361 years (only values 1, 2 or 3 are permitted)
     * @param   vahid           19-year-cycle (in range 1-19)
     * @param   yearOfVahid     year in range 1-19
     * @return  boolean
     * @throws  IllegalArgumentException in case of any inconsistencies
     */
    public static boolean isLeapYear(
        int kullishay,
        int vahid,
        int yearOfVahid
    ) {

        // TODO: implementieren
        return false;

    }

    /**
     * <p>Queries if given parameter values form a well defined calendar date. </p>
     *
     * @param   kullishay       major cycle of 361 years (only values 1, 2 or 3 are permitted)
     * @param   vahid           19-year-cycle (in range 1-19)
     * @param   yearOfVahid     year in range 1-19
     * @param   division        either {@code BadiMonth} or {@code BadiIntercalaryDays}
     * @param   day             day in range 1-19 (1-4/5 in case of Ayyam-i-Ha)
     * @return  {@code true} if valid else  {@code false}
     * @see     #ofComplete(int, int, int, BadiDivision, int)
     */
    /*[deutsch]
     * <p>Pr&uuml;ft, ob die angegebenen Parameter ein wohldefiniertes Kalenderdatum beschreiben. </p>
     *
     * @param   kullishay       major cycle of 361 years (only values 1, 2 or 3 are permitted)
     * @param   vahid           19-year-cycle (in range 1-19)
     * @param   yearOfVahid     year in range 1-19
     * @param   division        either {@code BadiMonth} or {@code BadiIntercalaryDays}
     * @param   day             day in range 1-19 (1-4/5 in case of Ayyam-i-Ha)
     * @return  {@code true} if valid else  {@code false}
     * @see     #ofComplete(int, int, int, BadiDivision, int)
     */
    public static boolean isValid(
        int kullishay,
        int vahid,
        int yearOfVahid,
        BadiDivision division,
        int day
    ) {

        if ((kullishay < 1) || (kullishay > 3)) {
            return false;
        } else if ((vahid < 1) || (vahid > 19)) {
            return false;
        } else if ((yearOfVahid < 1) || (yearOfVahid > 19)) {
            return false;
        }

        if (division instanceof BadiMonth) {
            return ((day >= 1) && (day <= 19));
        } else if (division == BadiIntercalaryDays.AYYAM_I_HA) {
            return ((day >= 1) && (day <= (isLeapYear(kullishay, vahid, yearOfVahid) ? 5 : 4)));
        } else {
            return false;
        }

    }

    /**
     * <p>Creates a new local timestamp with this date and given wall time. </p>
     *
     * <p>If the time {@link PlainTime#midnightAtEndOfDay() T24:00} is used
     * then the resulting timestamp will automatically be normalized such
     * that the timestamp will contain the following day instead. </p>
     *
     * @param   time    wall time
     * @return  general timestamp as composition of this date and given time
     */
    /*[deutsch]
     * <p>Erzeugt einen allgemeinen Zeitstempel mit diesem Datum und der angegebenen Uhrzeit. </p>
     *
     * <p>Wenn {@link PlainTime#midnightAtEndOfDay() T24:00} angegeben wird,
     * dann wird der Zeitstempel automatisch so normalisiert, da&szlig; er auf
     * den n&auml;chsten Tag verweist. </p>
     *
     * @param   time    wall time
     * @return  general timestamp as composition of this date and given time
     */
    public GeneralTimestamp<BadiCalendar> at(PlainTime time) {

        return GeneralTimestamp.of(this, time);

    }

    /**
     * <p>Is equivalent to {@code at(PlainTime.of(hour, minute))}. </p>
     *
     * @param   hour        hour of day in range (0-24)
     * @param   minute      minute of hour in range (0-59)
     * @return  general timestamp as composition of this date and given time
     * @throws  IllegalArgumentException if any argument is out of range
     */
    /*[deutsch]
     * <p>Entspricht {@code at(PlainTime.of(hour, minute))}. </p>
     *
     * @param   hour        hour of day in range (0-24)
     * @param   minute      minute of hour in range (0-59)
     * @return  general timestamp as composition of this date and given time
     * @throws  IllegalArgumentException if any argument is out of range
     */
    public GeneralTimestamp<BadiCalendar> atTime(
        int hour,
        int minute
    ) {

        return this.at(PlainTime.of(hour, minute));

    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        } else if (obj instanceof BadiCalendar) {
            BadiCalendar that = (BadiCalendar) obj;
            return (
                (this.major == that.major)
                    && (this.cycle == that.cycle)
                    && (this.year == that.year)
                    && (this.division == that.division)
                    && (this.day == that.day));
        } else {
            return false;
        }

    }

    @Override
    public int hashCode() {

        return (361 * this.major + 19 * this.cycle + this.year) * 512 + this.division * 19 + this.day;

    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder(32);
        sb.append("Bahai-");
        sb.append(this.major);
        sb.append('-');
        sb.append(this.cycle);
        sb.append('-');
        sb.append(this.year);
        sb.append('-');
        if (this.division == 0) {
            sb.append("Ayyam-i-Ha-");
        } else {
            sb.append(this.division);
            sb.append('-');
        }
        sb.append(this.day);
        return sb.toString();

    }

    @Override
    public boolean contains(ChronoElement<?> element) {

        if (element == MONTH_OF_YEAR) {
            return this.hasMonth();
        } else if (element == AYYAM_I_HA) {
            return this.isIntercalaryDay();
        } else if (this.getRegisteredElements().contains(element)) {
            return true;
        }

        // external element
        return isAccessible(this, element);

    }

    @Override
    public <V> boolean isValid(
        ChronoElement<V> element,
        V value
    ) {

        if ((element == MONTH_OF_YEAR) || (element == AYYAM_I_HA)) {
            return (value != null);
        }

        return super.isValid(element, value);

    }

    /**
     * <p>Returns the associated time axis. </p>
     *
     * @return  chronology
     */
    /*[deutsch]
     * <p>Liefert die zugeh&ouml;rige Zeitachse. </p>
     *
     * @return  chronology
     */
    public static TimeAxis<Unit, BadiCalendar> axis() {

        return ENGINE;

    }

    @Override
    protected TimeAxis<Unit, BadiCalendar> getChronology() {

        return ENGINE;

    }

    @Override
    protected BadiCalendar getContext() {

        return this;

    }

    /**
     * <p>Obtains the standard week model of this calendar. </p>
     *
     * <p>This calendar starts on Saturday (more precisely at sunset on Friday). </p>
     *
     * @return  Weekmodel
     */
    /*[deutsch]
     * <p>Ermittelt das Standardwochenmodell dieses Kalenders. </p>
     *
     * <p>Dieser Kalender startet am Samstag (eigentlich zum Sonnenuntergang am Freitag). </p>
     *
     * @return  Weekmodel
     */
    private static Weekmodel getDefaultWeekmodel() {

        return Weekmodel.of(Weekday.SATURDAY, 1, Weekday.SATURDAY, Weekday.SUNDAY);

    }

    private static <V> boolean isAccessible(
        BadiCalendar fcal,
        ChronoElement<V> element
    ) {

        try {
            return fcal.isValid(element, fcal.get(element));
        } catch (ChronoException ex) {
            return false;
        }

    }

    private static Locale getLocale(AttributeQuery attributes) {

        return attributes.get(Attributes.LANGUAGE, Locale.ROOT);

    }

    private static FormattedContent getFormattedContent(AttributeQuery attributes) {

        return attributes.get(TEXT_CONTENT_ATTRIBUTE, FormattedContent.TRANSCRIPTION);

    }

    /**
     * @serialData  Uses <a href="../../../../serialized-form.html#net.time4j.calendar.bahai/SPX">
     *              a dedicated serialization form</a> as proxy. The first byte contains
     *              the type-ID {@code 19}. Then the kull-i-shai-cycle, the vahid-cycle, the year of vahid
     *              and finally the month and day-of-month as bytes. Ayyam-i-Ha will be modelled as zero.
     *
     * @return  replacement object in serialization graph
     */
    private Object writeReplace() {

        return new SPX(this, SPX.BAHAI);

    }

    /**
     * @serialData  Blocks because a serialization proxy is required.
     * @param       in      object input stream
     * @throws InvalidObjectException (always)
     */
    private void readObject(ObjectInputStream in)
        throws IOException {

        throw new InvalidObjectException("Serialization proxy required.");

    }

    //~ Innere Klassen ----------------------------------------------------

    /**
     * <p>Defines come calendar units for the Badi calendar. </p>
     */
    /*[deutsch]
     * <p>Definiert einige kalendarische Zeiteinheiten f&uuml;r den Badi-Kalender. </p>
     */
    public static enum Unit
        implements ChronoUnit {

        //~ Statische Felder/Initialisierungen ----------------------------

        /**
         * <p>Cycles which last each 19 years. </p>
         */
        /*[deutsch]
         * <p>Jahreszyklen zu je 19 Jahren. </p>
         */
        VAHID_CYCLES(19 * 365.2424 * 86400.0),

        /**
         * <p>Years are defined as vernal equinox years and not as tropical years. </p>
         */
        /*[deutsch]
         * <p>Jahre starten zum Fr&uuml;hlingspunkt und sind nicht als tropische Jahre definiert. </p>
         */
        YEARS(365.2424 * 86400.0),

        /**
         * <p>The month arithmetic handles the intercalary days (Ayyam-i-Ha) as extension of eighteenth month. </p>
         */
        /*[deutsch]
         * <p>Die Monatsarithmetik behandelt die Erg&auml;nzungstage (Ayyam-i-Ha) als eine Erweiterung
         * des achtzehnten Monats. </p>
         */
        MONTHS(19 * 86400.0),

        /**
         * <p>Weeks consist of seven days. </p>
         */
        /*[deutsch]
         * <p>Wochen bestehen aus sieben Tagen. </p>
         */
        WEEKS(7 * 86400.0),

        /**
         * <p>The universal day unit. </p>
         */
        /*[deutsch]
         * <p>Die universelle Tageseinheit. </p>
         */
        DAYS(86400.0);

        //~ Instanzvariablen ----------------------------------------------

        private transient final double length;

        //~ Konstruktoren -------------------------------------------------

        private Unit(double length) {
            this.length = length;
        }

        //~ Methoden ------------------------------------------------------

        @Override
        public double getLength() {

            return this.length;

        }

        @Override
        public boolean isCalendrical() {

            return true;

        }

        /**
         * <p>Calculates the difference between given calendar dates in this unit. </p>
         *
         * @param   start   start date (inclusive)
         * @param   end     end date (exclusive)
         * @return  difference counted in this unit
         */
        /*[deutsch]
         * <p>Berechnet die Differenz zwischen den angegebenen Datumsparametern in dieser Zeiteinheit. </p>
         *
         * @param   start   start date (inclusive)
         * @param   end     end date (exclusive)
         * @return  difference counted in this unit
         */
        public long between(
            BadiCalendar start,
            BadiCalendar end
        ) {

            return start.until(end, this);

        }

    }

    private static class Transformer
        implements CalendarSystem<BadiCalendar> {

        //~ Statische Felder/Initialisierungen ----------------------------

        private static final ZonalOffset OFFSET =
            ZonalOffset.ofHoursMinutes(OffsetSign.AHEAD_OF_UTC, 3, 30);
        private static final long EPOCH =
            PlainDate.of(1844, 3, 21).getDaysSinceEpochUTC();
        private static final long SWITCH =
            PlainDate.of(2015, 3, 21).getDaysSinceEpochUTC();

        //~ Methoden ------------------------------------------------------

        @Override
        public BadiCalendar transform(long utcDays) {

            if (utcDays < SWITCH) {

            } else {
            }
            return new BadiCalendar(1, 1, 1, 1, 1); // TODO: implementieren

        }

        @Override
        public long transform(BadiCalendar date) {

            return 0L; // TODO: implementieren

        }

        @Override
        public long getMinimumSinceUTC() {

            BadiCalendar min = new BadiCalendar(1, 1, 1, 1, 1);
            return this.transform(min);

        }

        @Override
        public long getMaximumSinceUTC() {

            BadiCalendar max = new BadiCalendar(3, 19, 19, 19, 19);
            return this.transform(max);

        }

        @Override
        public List<CalendarEra> getEras() {

            return Collections.emptyList();

        }

    }

    private static class EraRule
        implements ElementRule<BadiCalendar, BadiEra> {

        //~ Methoden ------------------------------------------------------

        @Override
        public BadiEra getValue(BadiCalendar context) {

            return BadiEra.BAHAI;

        }

        @Override
        public BadiEra getMinimum(BadiCalendar context) {

            return BadiEra.BAHAI;

        }

        @Override
        public BadiEra getMaximum(BadiCalendar context) {

            return BadiEra.BAHAI;

        }

        @Override
        public boolean isValid(
            BadiCalendar context,
            BadiEra value
        ) {

            return (value != null);

        }

        @Override
        public BadiCalendar withValue(
            BadiCalendar context,
            BadiEra value,
            boolean lenient
        ) {

            if (value == null) {
                throw new IllegalArgumentException("Missing era value.");
            }

            return context;

        }

        @Override
        public ChronoElement<?> getChildAtFloor(BadiCalendar context) {

            return YEAR_OF_ERA;

        }

        @Override
        public ChronoElement<?> getChildAtCeiling(BadiCalendar context) {

            return YEAR_OF_ERA;

        }

    }

    private static class IntegerRule
        implements IntElementRule<BadiCalendar> {

        //~ Instanzvariablen ----------------------------------------------

        private final int index;

        //~ Konstruktoren -------------------------------------------------

        IntegerRule(int index) {
            super();

            this.index = index;

        }

        //~ Methoden ------------------------------------------------------

        @Override
        public int getInt(BadiCalendar context) {

            switch (this.index) {
                case KULL_I_SHAI_INDEX:
                    return context.major;
                case VAHID_INDEX:
                    return context.cycle;
                case YEAR_INDEX:
                    return context.year;
                case DAY_OF_DIVISION_INDEX:
                    return context.day;
                case DAY_OF_YEAR_INDEX:
                    return context.getDayOfYear();
                case YOE_INDEX:
                    return (context.major - 1) * 361 + (context.cycle - 1) * 19 + context.year;
                default:
                    throw new UnsupportedOperationException("Unknown element index: " + this.index);
            }

        }

        @Override
        public boolean isValid(BadiCalendar context, int value) {

            int max = this.getMax(context);
            return ((1 <= value) && (max >= value));

        }

        @Override
        public BadiCalendar withValue(BadiCalendar context, int value, boolean lenient) {

            if (!this.isValid(context, value)) {
                throw new IllegalArgumentException("Out of range: " + value);
            }

            int d = context.day;

            switch (this.index) {
                case KULL_I_SHAI_INDEX:
                    if (context.isIntercalaryDay() && (d == 5) && !isLeapYear(value, context.cycle, context.year)) {
                        d = 4;
                    }
                    return new BadiCalendar(value, context.cycle, context.year, context.division, d);
                case VAHID_INDEX:
                    if (context.isIntercalaryDay() && (d == 5) && !isLeapYear(context.major, value, context.year)) {
                        d = 4;
                    }
                    return new BadiCalendar(context.major, value, context.year, context.division, d);
                case YEAR_INDEX:
                    if (context.isIntercalaryDay() && (d == 5) && !isLeapYear(context.major, context.cycle, value)) {
                        d = 4;
                    }
                    return new BadiCalendar(context.major, context.cycle, value, context.division, d);
                case DAY_OF_DIVISION_INDEX:
                    return new BadiCalendar(context.major, context.cycle, context.year, context.division, value);
                case DAY_OF_YEAR_INDEX:
                    int pDiv;
                    int pDay;
                    if (value <= 18 * 19) {
                        pDiv = ((value - 1) / 19) + 1;
                        pDay = ((value - 1) % 19) + 1;
                    } else if (value <= 18 * 19 + (context.isLeapYear() ? 5 : 4)) {
                        pDiv = 0;
                        pDay = value - 18 * 19;
                    } else {
                        pDiv = 19;
                        pDay = value - (context.isLeapYear() ? 5 : 4) - 18 * 19;
                    }
                    return new BadiCalendar(context.major, context.cycle, context.year, pDiv, pDay);
                case YOE_INDEX:
                    int m = MathUtils.floorDivide(value - 1, 361) + 1;
                    int v = MathUtils.floorDivide(value - (m - 1) * 361 - 1, 19) + 1;
                    int yov = MathUtils.floorModulo(value - 1, 19) + 1;
                    if (context.isIntercalaryDay() && (d == 5) && !isLeapYear(m, v, yov)) {
                        d = 4;
                    }
                    return BadiCalendar.ofComplete(m, v, yov, context.getDivision(), d);
                default:
                    throw new UnsupportedOperationException("Unknown element index: " + this.index);
            }

        }

        @Override
        public Integer getValue(BadiCalendar context) {

            return Integer.valueOf(this.getInt(context));

        }

        @Override
        public Integer getMinimum(BadiCalendar context) {

            return Integer.valueOf(1);

        }

        @Override
        public Integer getMaximum(BadiCalendar context) {

            return Integer.valueOf(this.getMax(context));

        }

        @Override
        public boolean isValid(
            BadiCalendar context,
            Integer value
        ) {

            return ((value != null) && this.isValid(context, value.intValue()));

        }

        @Override
        public BadiCalendar withValue(
            BadiCalendar context,
            Integer value,
            boolean lenient
        ) {

            if (value == null) {
                throw new IllegalArgumentException("Missing new value.");
            }

            return this.withValue(context, value.intValue(), lenient);

        }

        @Override
        public ChronoElement<?> getChildAtFloor(BadiCalendar context) {

            switch (this.index) {
                case KULL_I_SHAI_INDEX:
                    return VAHID;
                case VAHID_INDEX:
                    return YEAR_OF_VAHID;
                case YEAR_INDEX:
                case YOE_INDEX:
                    return MONTH_OF_YEAR;
                case DAY_OF_DIVISION_INDEX:
                case DAY_OF_YEAR_INDEX:
                    return null;
                default:
                    throw new UnsupportedOperationException("Unknown element index: " + this.index);
            }

        }

        @Override
        public ChronoElement<?> getChildAtCeiling(BadiCalendar context) {

            switch (this.index) {
                case KULL_I_SHAI_INDEX:
                    return VAHID;
                case VAHID_INDEX:
                    return YEAR_OF_VAHID;
                case YEAR_INDEX:
                case YOE_INDEX:
                    return MONTH_OF_YEAR;
                case DAY_OF_DIVISION_INDEX:
                case DAY_OF_YEAR_INDEX:
                    return null;
                default:
                    throw new UnsupportedOperationException("Unknown element index: " + this.index);
            }

        }

        private int getMax(BadiCalendar context) {

            switch (this.index) {
                case KULL_I_SHAI_INDEX:
                    return 3;
                case VAHID_INDEX:
                case YEAR_INDEX:
                    return 19;
                case DAY_OF_DIVISION_INDEX:
                    if (context.isIntercalaryDay()) {
                        return context.isLeapYear() ? 5 : 4;
                    } else {
                        return 19;
                    }
                case DAY_OF_YEAR_INDEX:
                    return context.isLeapYear() ? 366 : 365;
                case YOE_INDEX:
                    return 3 * 361;
                default:
                    throw new UnsupportedOperationException("Unknown element index: " + this.index);
            }

        }

    }

    private static class MonthRule
        implements ElementRule<BadiCalendar, BadiMonth> {

        //~ Methoden ------------------------------------------------------

        @Override
        public BadiMonth getValue(BadiCalendar context) {

            return context.getMonth();

        }

        @Override
        public BadiMonth getMinimum(BadiCalendar context) {

            return BadiMonth.BAHA;

        }

        @Override
        public BadiMonth getMaximum(BadiCalendar context) {

            return BadiMonth.ALA;

        }

        @Override
        public boolean isValid(
            BadiCalendar context,
            BadiMonth value
        ) {

            return (value != null);

        }

        @Override
        public BadiCalendar withValue(
            BadiCalendar context,
            BadiMonth value,
            boolean lenient
        ) {

            if (value == null) {
                throw new IllegalArgumentException("Missing Badi month.");
            } else {
                int d = context.isIntercalaryDay() ? 19 : context.day;
                return new BadiCalendar(context.major, context.cycle, context.year, value.getValue(), d);
            }

        }

        @Override
        public ChronoElement<?> getChildAtFloor(BadiCalendar context) {

            return DAY_OF_DIVISION;

        }

        @Override
        public ChronoElement<?> getChildAtCeiling(BadiCalendar context) {

            return DAY_OF_DIVISION;

        }

    }

    private static class MonthElement
        extends StdEnumDateElement<BadiMonth, BadiCalendar> {

        //~ Statische Felder/Initialisierungen ----------------------------

        static final MonthElement SINGLETON = new MonthElement();

        // TODO: serialver
        //private static final long serialVersionUID = -8211850819064695450L;

        //~ Konstruktoren -------------------------------------------------

        private MonthElement() {
            super("MONTH_OF_YEAR", BadiCalendar.class, BadiMonth.class, 'M');

        }

        //~ Methoden ------------------------------------------------------

        @Override
        protected boolean isSingleton() {

            return true;

        }

        @Override
        public void print(
            ChronoDisplay context,
            Appendable buffer,
            AttributeQuery attributes
        ) throws IOException {

            BadiMonth value = context.get(this);
            buffer.append(this.accessor(attributes).print(value));

        }

        @Override
        public BadiMonth parse(
            CharSequence text,
            ParsePosition status,
            AttributeQuery attributes
        ) {

            return this.accessor(attributes).parse(text, status, BadiMonth.class, attributes);

        }

        private TextAccessor accessor(AttributeQuery attributes) {

            Locale lang = getLocale(attributes);
            FormattedContent fc = getFormattedContent(attributes);
            CalendarText ct = CalendarText.getInstance("extra/bahai", lang);
            return ct.getTextForms("M", BadiMonth.class, fc.variant());

        }

    }

    private static class DowElement
        extends StdWeekdayElement<BadiCalendar> {

        //~ Statische Felder/Initialisierungen ----------------------------

        static final DowElement SINGLETON = new DowElement();

        // TODO: serialver
        //private static final long serialVersionUID = -8211850819064695450L;

        //~ Konstruktoren -------------------------------------------------

        private DowElement() {
            super(BadiCalendar.class, BadiCalendar.getDefaultWeekmodel());

        }

        //~ Methoden ------------------------------------------------------

        @Override
        protected boolean isSingleton() {

            return true;

        }

        @Override
        public void print(
            ChronoDisplay context,
            Appendable buffer,
            AttributeQuery attributes
        ) throws IOException {

            Weekday value = context.get(this);
            buffer.append(this.accessor(attributes).print(value));

        }

        @Override
        public Weekday parse(
            CharSequence text,
            ParsePosition status,
            AttributeQuery attributes
        ) {

            return this.accessor(attributes).parse(text, status, Weekday.class, attributes);

        }

        private TextAccessor accessor(AttributeQuery attributes) {

            Locale lang = getLocale(attributes);
            FormattedContent fc = getFormattedContent(attributes);
            CalendarText ct = CalendarText.getInstance("extra/bahai", lang);
            return ct.getTextForms("D", Weekday.class, fc.variant());

        }

    }

    private static class YOV
        extends DisplayElement<Integer>
        implements TextElement<Integer> {

        //~ Statische Felder/Initialisierungen ----------------------------

        static final YOV SINGLETON = new YOV();

        // TODO: serialver
        //private static final long serialVersionUID = -8211850819064695450L;

        //~ Konstruktoren -------------------------------------------------

        private YOV() {
            super("YEAR_OF_VAHID");
        }

        //~ Methoden ------------------------------------------------------

        @Override
        public char getSymbol() {

            return 'Y';

        }

        @Override
        public void print(
            ChronoDisplay context,
            Appendable buffer,
            AttributeQuery attributes
        ) throws IOException, ChronoException {

            int value = context.getInt(this);
            Enum<?> e = enumAccess().getEnumConstants()[value - 1];
            buffer.append(this.accessor(attributes).print(e));

        }

        @Override
        public Integer parse(
            CharSequence text,
            ParsePosition status,
            AttributeQuery attributes
        ) {

            Enum<?> e = this.accessor(attributes).parse(text, status, enumAccess(), attributes);

            if (e == null) {
                return null;
            } else {
                return Integer.valueOf(e.ordinal() + 1);
            }

        }

        @Override
        public Class<Integer> getType() {

            return Integer.class;

        }

        @Override
        public Integer getDefaultMinimum() {

            return Integer.valueOf(1);

        }

        @Override
        public Integer getDefaultMaximum() {

            return Integer.valueOf(19);

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
        protected boolean isSingleton() {

            return true;

        }

        private TextAccessor accessor(AttributeQuery attributes) {

            Locale lang = getLocale(attributes);
            FormattedContent fc = getFormattedContent(attributes);
            CalendarText ct = CalendarText.getInstance("extra/bahai", lang);
            return ct.getTextForms("YOV", enumAccess(), fc.variant());

        }

        private static Class<BadiMonth> enumAccess() {

            // uses BadiMonth-enum as intermediate helper class only (it has 19 instances, too)
            return BadiMonth.class;

        }

    }

    private static class IntercalaryAccess
        extends BasicElement<BadiIntercalaryDays>
        implements TextElement<BadiIntercalaryDays>, ElementRule<BadiCalendar, BadiIntercalaryDays> {

        //~ Statische Felder/Initialisierungen ----------------------------

        static final IntercalaryAccess SINGLETON = new IntercalaryAccess();

        // TODO: serialver
        //private static final long serialVersionUID = -8211850819064695450L;

        //~ Konstruktoren -------------------------------------------------

        private IntercalaryAccess() {
            super("AYYAM_I_HA");
        }

        //~ Methoden ------------------------------------------------------

        @Override
        public char getSymbol() {

            return 'A';

        }

        @Override
        public BadiIntercalaryDays getValue(BadiCalendar context) {

            if (context.isIntercalaryDay()) {
                return BadiIntercalaryDays.AYYAM_I_HA;
            } else {
                throw new ChronoException("The actual calendar date is not an intercalary day: " + context);
            }

        }

        @Override
        public BadiIntercalaryDays getMinimum(BadiCalendar context) {

            return BadiIntercalaryDays.AYYAM_I_HA;

        }

        @Override
        public BadiIntercalaryDays getMaximum(BadiCalendar context) {

            return BadiIntercalaryDays.AYYAM_I_HA;

        }

        @Override
        public boolean isValid(
            BadiCalendar context,
            BadiIntercalaryDays value
        ) {

            return (value == BadiIntercalaryDays.AYYAM_I_HA);

        }

        @Override
        public BadiCalendar withValue(
            BadiCalendar context,
            BadiIntercalaryDays value,
            boolean lenient
        ) {

            if (value != BadiIntercalaryDays.AYYAM_I_HA) {
                throw new IllegalArgumentException("Expected Ayyam-i-Ha: " + value);
            }

            int d = Math.min(context.day, context.isLeapYear() ? 5 : 4);
            return new BadiCalendar(context.major, context.cycle, context.year, 0, d);

        }

        @Override
        public ChronoElement<?> getChildAtFloor(BadiCalendar context) {

            return DAY_OF_DIVISION;

        }

        @Override
        public ChronoElement<?> getChildAtCeiling(BadiCalendar context) {

            return DAY_OF_DIVISION;

        }

        @Override
        public void print(
            ChronoDisplay context,
            Appendable buffer,
            AttributeQuery attributes
        ) throws IOException, ChronoException {

            BadiIntercalaryDays value = context.get(this);
            Locale lang = attributes.get(Attributes.LANGUAGE, Locale.ROOT);
            buffer.append(this.accessor(lang, attributes).print(value));

        }

        @Override
        public BadiIntercalaryDays parse(
            CharSequence text,
            ParsePosition status,
            AttributeQuery attributes
        ) {

            Locale lang = attributes.get(Attributes.LANGUAGE, Locale.ROOT);
            return this.accessor(lang, attributes).parse(text, status, this.getType(), attributes);

        }

        @Override
        public Class<BadiIntercalaryDays> getType() {

            return BadiIntercalaryDays.class;

        }

        @Override
        public BadiIntercalaryDays getDefaultMinimum() {

            return BadiIntercalaryDays.AYYAM_I_HA;

        }

        @Override
        public BadiIntercalaryDays getDefaultMaximum() {

            return BadiIntercalaryDays.AYYAM_I_HA;

        }

        @Override
        public String getDisplayName(Locale language) {

            return BadiIntercalaryDays.AYYAM_I_HA.getDisplayName(language);

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
        protected boolean isSingleton() {

            return true;

        }

        private TextAccessor accessor(
            Locale lang,
            AttributeQuery attributes
        ) {


            FormattedContent fc = attributes.get(TEXT_CONTENT_ATTRIBUTE, FormattedContent.TRANSCRIPTION);
            CalendarText ct = CalendarText.getInstance("extra/bahai", lang);
            String nameKey = "A";

            if ((fc == FormattedContent.MEANING) && ct.getTextForms().containsKey("a")) {
                nameKey = "a";
            }

            return ct.getTextForms(nameKey, this.getType());

        }

    }

    private static class WeekdayRule
        implements ElementRule<BadiCalendar, Weekday> {

        //~ Methoden ------------------------------------------------------

        @Override
        public Weekday getValue(BadiCalendar context) {

            return context.getDayOfWeek();

        }

        @Override
        public Weekday getMinimum(BadiCalendar context) {

            // TODO: Grenzwerte richtig stellen
            return ((context.year == 1) && (context.getDayOfYear() == 1)) ? Weekday.SATURDAY : Weekday.SUNDAY;

        }

        @Override
        public Weekday getMaximum(BadiCalendar context) {

            // TODO: Grenzwerte richtig stellen
            return ((context.year == 500) && (context.getDayOfYear() == 366)) ? Weekday.SUNDAY : Weekday.SATURDAY;

        }

        @Override
        public boolean isValid(
            BadiCalendar context,
            Weekday value
        ) {

            if (value == null) {
                return false;
            }

            Weekmodel model = getDefaultWeekmodel();
            int w = value.getValue(model);
            int wMin = this.getMinimum(context).getValue(model);
            int wMax = this.getMaximum(context).getValue(model);
            return ((wMin <= w) && (w <= wMax));

        }

        @Override
        public BadiCalendar withValue(
            BadiCalendar context,
            Weekday value,
            boolean lenient
        ) {

            if (value == null) {
                throw new IllegalArgumentException("Missing weekday.");
            }

            Weekmodel model = getDefaultWeekmodel();
            int oldValue = context.getDayOfWeek().getValue(model);
            int newValue = value.getValue(model);
            return context.plus(CalendarDays.of(newValue - oldValue));

        }

        @Override
        public ChronoElement<?> getChildAtFloor(BadiCalendar context) {

            return null;

        }

        @Override
        public ChronoElement<?> getChildAtCeiling(BadiCalendar context) {

            return null;

        }

    }

    private static class Merger
        implements ChronoMerger<BadiCalendar> {

        //~ Methoden ------------------------------------------------------

        @Override
        public BadiCalendar createFrom(
            TimeSource<?> clock,
            AttributeQuery attributes
        ) {

            TZID tzid;

            if (attributes.contains(Attributes.TIMEZONE_ID)) {
                tzid = attributes.get(Attributes.TIMEZONE_ID);
            } else if (attributes.get(Attributes.LENIENCY, Leniency.SMART).isLax()) {
                tzid = Timezone.ofSystem().getID();
            } else {
                return null;
            }

            StartOfDay startOfDay = attributes.get(Attributes.START_OF_DAY, this.getDefaultStartOfDay());
            return Moment.from(clock.currentTime()).toGeneralTimestamp(ENGINE, tzid, startOfDay).toDate();

        }

        @Override
        public BadiCalendar createFrom(
            ChronoEntity<?> entity,
            AttributeQuery attributes,
            boolean lenient,
            boolean preparsing
        ) {

            int major = entity.getInt(KULL_I_SHAI);

            if (major == Integer.MIN_VALUE) {
                major = 1; // smart parsing
            } else if ((major < 1) || (major > 3)) {
                entity.with(ValidationElement.ERROR_MESSAGE, "Major cycle out of range: " + major);
                return null;
            }

            int vahid = entity.getInt(VAHID);
            boolean hasVahid = true;

            if (vahid == Integer.MIN_VALUE) {
                hasVahid = false;
            } else if ((vahid < 1) || (vahid > 19)) {
                entity.with(ValidationElement.ERROR_MESSAGE, "Vahid cycle out of range: " + vahid);
                return null;
            }

            int year = entity.getInt(YEAR_OF_VAHID);

            if (year == Integer.MIN_VALUE) {
                if (hasVahid) {
                    entity.with(ValidationElement.ERROR_MESSAGE, "Missing year-of-vahid.");
                    return null;
                } else if (entity.contains(ERA) && entity.contains(YEAR_OF_ERA)) {
                    BadiCalendar prototype =
                        BadiCalendar.axis().getMinimum().with(YEAR_OF_ERA, entity.getInt(YEAR_OF_ERA));
                    major = prototype.getKullishai();
                    vahid = prototype.getVahid();
                    year = prototype.getYearOfVahid();
                } else {
                    entity.with(ValidationElement.ERROR_MESSAGE, "Missing vahid cycle.");
                    return null;
                }
            } else if ((year < 1) || (year > 19)) {
                entity.with(ValidationElement.ERROR_MESSAGE, "Badi year-of-vahid out of range: " + year);
                return null;
            }

            BadiCalendar cal = null;

            if (entity.contains(MONTH_OF_YEAR)) {
                int month = entity.get(MONTH_OF_YEAR).getValue();
                int dom = entity.getInt(DAY_OF_DIVISION);

                if ((dom >= 1) && (dom <= 19)) {
                    cal = new BadiCalendar(major, vahid, year, month, dom);
                } else {
                    entity.with(ValidationElement.ERROR_MESSAGE, "Invalid Badi date.");
                }
            } else if (entity.contains(AYYAM_I_HA)) {
                int day = entity.getInt(DAY_OF_DIVISION);

                if ((day >= 1) && (day <= (isLeapYear(major, vahid, year) ? 5 : 4))) {
                    cal = new BadiCalendar(major, vahid, year, 0, day);
                } else {
                    entity.with(ValidationElement.ERROR_MESSAGE, "Invalid Badi date.");
                }
            } else {
                int doy = entity.getInt(DAY_OF_YEAR);
                boolean leap = isLeapYear(major, vahid, year);
                if (doy != Integer.MIN_VALUE) {
                    if ((doy >= 1) && (doy <= (leap ? 366 : 365))) {
                        int pDiv;
                        int pDay;
                        if (doy <= 18 * 19) {
                            pDiv = ((doy - 1) / 19) + 1;
                            pDay = ((doy - 1) % 19) + 1;
                        } else if (doy <= 18 * 19 + (leap ? 5 : 4)) {
                            pDiv = 0;
                            pDay = doy - 18 * 19;
                        } else {
                            pDiv = 19;
                            pDay = doy - (leap ? 5 : 4) - 18 * 19;
                        }
                        cal = new BadiCalendar(major, vahid, year, pDiv, pDay);
                    } else {
                        entity.with(ValidationElement.ERROR_MESSAGE, "Invalid Badi date.");
                    }
                }
            }

            return cal;

        }

        @Override
        public ChronoDisplay preformat(BadiCalendar context, AttributeQuery attributes) {

            return context;

        }

        @Override
        public int getDefaultPivotYear() {

            return PlainDate.axis().getDefaultPivotYear() - 1844; // not relevant for dynamic pattern type

        }

        @Override
        public StartOfDay getDefaultStartOfDay() {

            return StartOfDay.definedBy(TEHERAN.sunset());

        }

    }

    private static class FUnitRule
        implements UnitRule<BadiCalendar> {

        //~ Instanzvariablen ----------------------------------------------

        private final Unit unit;

        //~ Konstruktoren -------------------------------------------------

        FUnitRule(Unit unit) {
            super();

            this.unit = unit;

        }

        //~ Methoden ------------------------------------------------------

        @Override
        public BadiCalendar addTo(BadiCalendar date, long amount) {

            switch (this.unit) {
                case VAHID_CYCLES:
                    amount = MathUtils.safeMultiply(amount, 19);
                    // fall-through
                case YEARS:
                    long yy = MathUtils.safeAdd(elapsedYears(date), amount);
                    int majorY = MathUtils.safeCast(MathUtils.floorDivide(yy, 361)) + 1;
                    int remainderY = MathUtils.floorModulo(yy, 361);
                    int cycleY = MathUtils.floorDivide(remainderY, 19) + 1;
                    int yearY = MathUtils.floorModulo(remainderY, 19) + 1;
                    int dod = date.day;
                    if ((date.day == 5) && !isLeapYear(majorY, cycleY, yearY)) {
                        dod = 4;
                    }
                    return BadiCalendar.ofComplete(majorY, cycleY, yearY, date.getDivision(), dod);
                case MONTHS: // interprete ayyam-i-ha as extension of month 18
                    long ym = MathUtils.safeAdd(elapsedMonths(date), amount);
                    int majorM = MathUtils.safeCast(MathUtils.floorDivide(ym, 6859)) + 1;
                    int remainderM = MathUtils.floorModulo(ym, 6859);
                    int cycleM = MathUtils.floorDivide(remainderM, 361) + 1;
                    remainderM = MathUtils.floorModulo(remainderM, 361);
                    int yearM = MathUtils.floorDivide(remainderM, 19) + 1;
                    int month = MathUtils.floorModulo(remainderM, 19) + 1;
                    int dom = (date.isIntercalaryDay() ? 19 : date.day);
                    return BadiCalendar.ofComplete(majorM, cycleM, yearM, BadiMonth.valueOf(month), dom);
                case WEEKS:
                    amount = MathUtils.safeMultiply(amount, 7);
                    // fall-through
                case DAYS:
                    long utcDays = MathUtils.safeAdd(CALSYS.transform(date), amount);
                    return CALSYS.transform(utcDays);
                default:
                    throw new UnsupportedOperationException(this.unit.name());
            }

        }

        @Override
        public long between(BadiCalendar start, BadiCalendar end) {

            switch (this.unit) {
                case VAHID_CYCLES:
                    return BadiCalendar.Unit.YEARS.between(start, end) / 19;
                case YEARS:
                    int deltaY = elapsedYears(end) - elapsedYears(start);
                    if ((deltaY > 0) && (end.getDayOfYear() < start.getDayOfYear())) {
                        deltaY--;
                    } else if ((deltaY < 0) && (end.getDayOfYear() > start.getDayOfYear())) {
                        deltaY++;
                    }
                    return deltaY;
                case MONTHS: // interprete ayyam-i-ha as extension of month 18
                    long deltaM = elapsedMonths(end) - elapsedMonths(start);
                    int sdom = (start.isIntercalaryDay() ? (start.day + 19) : start.day);
                    int edom = (end.isIntercalaryDay() ? (end.day + 19) : end.day);
                    if ((deltaM > 0) && (edom < sdom)) {
                        deltaM--;
                    } else if ((deltaM < 0) && (edom > sdom)) {
                        deltaM++;
                    }
                    return deltaM;
                case WEEKS:
                    return BadiCalendar.Unit.DAYS.between(start, end) / 7;
                case DAYS:
                    return CALSYS.transform(end) - CALSYS.transform(start);
                default:
                    throw new UnsupportedOperationException(this.unit.name());
            }

        }

        private static int elapsedYears(BadiCalendar date) {
            return (((date.major - 1) * 19) + (date.cycle - 1)) * 19 + date.year - 1;
        }

        private static int elapsedMonths(BadiCalendar date) {
            int m = (date.isIntercalaryDay() ? 18 : date.getMonth().getValue());
            return 19 * elapsedYears(date) + m - 1;
        }

    }

}
