/*
 * Copyright 2015 - 2016 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class XirgoProtocolDecoder extends BaseProtocolDecoder {

    public XirgoProtocolDecoder(XirgoProtocol protocol) {
        super(protocol);
    }

    private Boolean newFormat;

    private static final Pattern PATTERN_OLD = new PatternBuilder()
            .text("$$")
            .number("(d+),")                     // imei
            .number("(d+),")                     // event
            .number("(dddd)/(dd)/(dd),")         // date
            .number("(dd):(dd):(dd),")           // time
            .number("(-?d+.?d*),")               // latitude
            .number("(-?d+.?d*),")               // longitude
            .number("(-?d+.?d*),")               // altitude
            .number("(d+.?d*),")                 // speed
            .number("(d+.?d*),")                 // course
            .number("(d+),")                     // satellites
            .number("(d+.?d*),")                 // hdop
            .number("(d+.d+),")                  // battery
            .number("(d+),")                     // gsm
            .number("(d+.?d*),")                 // odometer
            .number("(d+),")                     // gps
            .any()
            .compile();

    private static final Pattern PATTERN_NEW = new PatternBuilder()
            .text("$$")
            .number("(d+),")                     // imei
            .number("(d+),")                     // event
            .number("(dddd)/(dd)/(dd),")         // date (yyyy/mm/dd)
            .number("(dd):(dd):(dd),")           // time (hh:mm:ss)
            .number("(-?d+.?d*),")               // latitude
            .number("(-?d+.?d*),")               // longitude
            .number("(-?d+.?d*),")               // altitude
            .number("(d+.?d*),")                 // speed
            .number("d+.?d*,")                   // acceleration
            .number("d+.?d*,")                   // deceleration
            .number("d+,")
            .number("(d+.?d*),")                 // course
            .number("(d+),")                     // satellites
            .number("(d+.?d*),")                 // hdop
            .number("(d+.?d*),")                 // odometer
            .number("(d+.?d*),")                 // fuel consumption
            .number("(d+.d+),")                  // battery
            .number("(d+),")                     // gsm
            .number("(d+),")                     // gps
            .any()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        Parser parser;
        if (newFormat == null) {
            parser = new Parser(PATTERN_NEW, sentence);
            if (parser.matches()) {
                newFormat = true;
            } else {
                parser = new Parser(PATTERN_OLD, sentence);
                if (parser.matches()) {
                    newFormat = false;
                } else {
                    return null;
                }
            }
        } else {
            if (newFormat) {
                parser = new Parser(PATTERN_NEW, sentence);
            } else {
                parser = new Parser(PATTERN_OLD, sentence);
            }
            if (!parser.matches()) {
                return null;
            }
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_EVENT, parser.next());

        position.setTime(parser.nextDateTime());

        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());
        position.setAltitude(parser.nextDouble());
        position.setSpeed(UnitsConverter.knotsFromMph(parser.nextDouble()));
        position.setCourse(parser.nextDouble());

        position.set(Position.KEY_SATELLITES, parser.next());
        position.set(Position.KEY_HDOP, parser.next());

        if (newFormat) {
            position.set(Position.KEY_ODOMETER, parser.nextDouble() * 1609.34);
            position.set(Position.KEY_FUEL_CONSUMPTION, parser.next());
        }

        position.set(Position.KEY_BATTERY, parser.next());
        position.set(Position.KEY_RSSI, parser.next());

        if (!newFormat) {
            position.set(Position.KEY_ODOMETER, parser.nextDouble() * 1609.34);
        }

        position.setValid(parser.nextInt() == 1);

        return position;
    }

}
