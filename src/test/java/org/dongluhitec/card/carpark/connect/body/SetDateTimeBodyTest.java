package org.dongluhitec.card.carpark.connect.body;

import org.dongluhitec.card.carpark.connect.util.ByteUtils;
import org.joda.time.DateTime;
import org.junit.Assert;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Created by xiaopan on 2016-07-19.
 */
public class SetDateTimeBodyTest {

    public static void main(String[] args) {
        DateTime dateTime = new DateTime(2016, 7, 19, 14, 35, 40);
        SetDateTimeBody setDateTimeBody = new SetDateTimeBody(dateTime.toDate());
        String format = ByteUtils.byteArrayToHexString(setDateTimeBody.toBytes());
        Assert.assertEquals("[16 07 19 02 14 35]",format);
    }

}