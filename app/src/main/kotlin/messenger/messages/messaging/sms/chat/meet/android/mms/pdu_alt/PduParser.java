package messenger.messages.messaging.sms.chat.meet.android.mms.pdu_alt;

import messenger.messages.messaging.sms.chat.meet.mms.util.ExternalLogger;
import messenger.messages.messaging.sms.chat.meet.android.mms.ContentType;
import messenger.messages.messaging.sms.chat.meet.android.mms.InvalidHeaderValueException;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;

public class PduParser {

    private static final int QUOTE = 127;
    private static final int LENGTH_QUOTE = 31;
    private static final int TEXT_MIN = 32;
    private static final int TEXT_MAX = 127;
    private static final int SHORT_INTEGER_MAX = 127;
    private static final int SHORT_LENGTH_MAX = 30;
    private static final int LONG_INTEGER_LENGTH_MAX = 8;
    private static final int QUOTED_STRING_FLAG = 34;
    private static final int END_STRING_FLAG = 0x00;
    //The next two are used by the interface "parseWapString" to
    //distinguish Text-String and Quoted-String.
    private static final int TYPE_TEXT_STRING = 0;
    private static final int TYPE_QUOTED_STRING = 1;
    private static final int TYPE_TOKEN_STRING = 2;

    private static final int THE_FIRST_PART = 0;
    private static final int THE_LAST_PART = 1;

    private ByteArrayInputStream mPduDataStream = null;

    private PduHeaders mHeaders = null;

    private PduBody mBody = null;

    private static byte[] mTypeParam = null;

    private static byte[] mStartParam = null;

    private static final String LOG_TAG = "PduParser";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = false;

    private final boolean mParseContentDisposition;

    public PduParser(byte[] pduDataStream, boolean parseContentDisposition) {
        mPduDataStream = new ByteArrayInputStream(pduDataStream);
        mParseContentDisposition = parseContentDisposition;
    }

    public PduParser(byte[] pduDataStream) {
        this(pduDataStream, true);
    }

    public GenericPdu parse(){
        if (mPduDataStream == null) {
            return null;
        }

        /* parse headers */
        mHeaders = parseHeaders(mPduDataStream);
        if (null == mHeaders) {
            // Parse headers failed.
            return null;
        }

        /* get the message type */
        int messageType = mHeaders.getOctet(PduHeaders.MESSAGE_TYPE);

        /* check mandatory header fields */
        if (false == checkMandatoryHeader(mHeaders)) {
            log("check mandatory headers failed!");
            return null;
        }

        if ((PduHeaders.MESSAGE_TYPE_SEND_REQ == messageType) ||
                (PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF == messageType)) {
            /* need to parse the parts */
            mBody = parseParts(mPduDataStream);
            if (null == mBody) {
                // Parse parts failed.
                return null;
            }
        }

        switch (messageType) {
            case PduHeaders.MESSAGE_TYPE_SEND_REQ:
                if (LOCAL_LOGV) {
                    Log.v(LOG_TAG, "parse: MESSAGE_TYPE_SEND_REQ");
                }
                SendReq sendReq = new SendReq(mHeaders, mBody);
                return sendReq;
            case PduHeaders.MESSAGE_TYPE_SEND_CONF:
                if (LOCAL_LOGV) {
                    Log.v(LOG_TAG, "parse: MESSAGE_TYPE_SEND_CONF");
                }
                SendConf sendConf = new SendConf(mHeaders);
                return sendConf;
            case PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND:
                if (LOCAL_LOGV) {
                    Log.v(LOG_TAG, "parse: MESSAGE_TYPE_NOTIFICATION_IND");
                }
                NotificationInd notificationInd =
                    new NotificationInd(mHeaders);
                return notificationInd;
            case PduHeaders.MESSAGE_TYPE_NOTIFYRESP_IND:
                if (LOCAL_LOGV) {
                    Log.v(LOG_TAG, "parse: MESSAGE_TYPE_NOTIFYRESP_IND");
                }
                NotifyRespInd notifyRespInd =
                    new NotifyRespInd(mHeaders);
                return notifyRespInd;
            case PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF:
                if (LOCAL_LOGV) {
                    Log.v(LOG_TAG, "parse: MESSAGE_TYPE_RETRIEVE_CONF");
                }
                RetrieveConf retrieveConf =
                    new RetrieveConf(mHeaders, mBody);

                byte[] contentType = retrieveConf.getContentType();
                if (null == contentType) {
                    return null;
                }
                String ctTypeStr = new String(contentType);
                if (ctTypeStr.equals(ContentType.MULTIPART_MIXED)
                        || ctTypeStr.equals(ContentType.MULTIPART_RELATED)
                        || ctTypeStr.equals(ContentType.MULTIPART_ALTERNATIVE)) {
                    // The MMS content type must be "application/vnd.wap.multipart.mixed"
                    // or "application/vnd.wap.multipart.related"
                    // or "application/vnd.wap.multipart.alternative"
                    return retrieveConf;
                } else if (ctTypeStr.equals(ContentType.MULTIPART_ALTERNATIVE)) {
                    // "application/vnd.wap.multipart.alternative"
                    // should take only the first part.
                    PduPart firstPart = mBody.getPart(0);
                    mBody.removeAll();
                    mBody.addPart(0, firstPart);
                    return retrieveConf;
                } else if (ctTypeStr.equals(ContentType.MULTIPART_SIGNED)) {
                    // multipart/signed
                    return retrieveConf;
                } else {
                    ExternalLogger.logMessage(LOG_TAG, "Unsupported ContentType: " + ctTypeStr);
                }
                return null;
            case PduHeaders.MESSAGE_TYPE_DELIVERY_IND:
                if (LOCAL_LOGV) {
                    Log.v(LOG_TAG, "parse: MESSAGE_TYPE_DELIVERY_IND");
                }
                DeliveryInd deliveryInd =
                    new DeliveryInd(mHeaders);
                return deliveryInd;
            case PduHeaders.MESSAGE_TYPE_ACKNOWLEDGE_IND:
                if (LOCAL_LOGV) {
                    Log.v(LOG_TAG, "parse: MESSAGE_TYPE_ACKNOWLEDGE_IND");
                }
                AcknowledgeInd acknowledgeInd =
                    new AcknowledgeInd(mHeaders);
                return acknowledgeInd;
            case PduHeaders.MESSAGE_TYPE_READ_ORIG_IND:
                if (LOCAL_LOGV) {
                    Log.v(LOG_TAG, "parse: MESSAGE_TYPE_READ_ORIG_IND");
                }
                ReadOrigInd readOrigInd =
                    new ReadOrigInd(mHeaders);
                return readOrigInd;
            case PduHeaders.MESSAGE_TYPE_READ_REC_IND:
                if (LOCAL_LOGV) {
                    Log.v(LOG_TAG, "parse: MESSAGE_TYPE_READ_REC_IND");
                }
                ReadRecInd readRecInd =
                    new ReadRecInd(mHeaders);
                return readRecInd;
            default:
                log("Parser doesn't support this message type in this version!");
            return null;
        }
    }

    protected PduHeaders parseHeaders(ByteArrayInputStream pduDataStream){
        if (pduDataStream == null) {
            return null;
        }
        boolean keepParsing = true;
        PduHeaders headers = new PduHeaders();

        while (keepParsing && (pduDataStream.available() > 0)) {
            pduDataStream.mark(1);
            int headerField = extractByteValue(pduDataStream);
            /* parse custom text header */
            if ((headerField >= TEXT_MIN) && (headerField <= TEXT_MAX)) {
                pduDataStream.reset();
                byte [] bVal = parseWapString(pduDataStream, TYPE_TEXT_STRING);
                if (LOCAL_LOGV) {
                    Log.v(LOG_TAG, "TextHeader: " + new String(bVal));
                }
                /* we should ignore it at the moment */
                continue;
            }
            switch (headerField) {
                case PduHeaders.MESSAGE_TYPE:
                {
                    int messageType = extractByteValue(pduDataStream);
                    if (LOCAL_LOGV) {
                        Log.v(LOG_TAG, "parseHeaders: messageType: " + messageType);
                    }
                    switch (messageType) {
                        // We don't support these kind of messages now.
                        case PduHeaders.MESSAGE_TYPE_FORWARD_REQ:
                        case PduHeaders.MESSAGE_TYPE_FORWARD_CONF:
                        case PduHeaders.MESSAGE_TYPE_MBOX_STORE_REQ:
                        case PduHeaders.MESSAGE_TYPE_MBOX_STORE_CONF:
                        case PduHeaders.MESSAGE_TYPE_MBOX_VIEW_REQ:
                        case PduHeaders.MESSAGE_TYPE_MBOX_VIEW_CONF:
                        case PduHeaders.MESSAGE_TYPE_MBOX_UPLOAD_REQ:
                        case PduHeaders.MESSAGE_TYPE_MBOX_UPLOAD_CONF:
                        case PduHeaders.MESSAGE_TYPE_MBOX_DELETE_REQ:
                        case PduHeaders.MESSAGE_TYPE_MBOX_DELETE_CONF:
                        case PduHeaders.MESSAGE_TYPE_MBOX_DESCR:
                        case PduHeaders.MESSAGE_TYPE_DELETE_REQ:
                        case PduHeaders.MESSAGE_TYPE_DELETE_CONF:
                        case PduHeaders.MESSAGE_TYPE_CANCEL_REQ:
                        case PduHeaders.MESSAGE_TYPE_CANCEL_CONF:
                            return null;
                    }
                    try {
                        headers.setOctet(messageType, headerField);
                    } catch(InvalidHeaderValueException e) {
                        log("Set invalid Octet value: " + messageType +
                                " into the header filed: " + headerField);
                        return null;
                    } catch(RuntimeException e) {
                        log(headerField + "is not Octet header field!");
                        return null;
                    }
                    break;
                }
                /* Octect value */
                case PduHeaders.REPORT_ALLOWED:
                case PduHeaders.ADAPTATION_ALLOWED:
                case PduHeaders.DELIVERY_REPORT:
                case PduHeaders.DRM_CONTENT:
                case PduHeaders.DISTRIBUTION_INDICATOR:
                case PduHeaders.QUOTAS:
                case PduHeaders.READ_REPORT:
                case PduHeaders.STORE:
                case PduHeaders.STORED:
                case PduHeaders.TOTALS:
                case PduHeaders.SENDER_VISIBILITY:
                case PduHeaders.READ_STATUS:
                case PduHeaders.CANCEL_STATUS:
                case PduHeaders.PRIORITY:
                case PduHeaders.STATUS:
                case PduHeaders.REPLY_CHARGING:
                case PduHeaders.MM_STATE:
                case PduHeaders.RECOMMENDED_RETRIEVAL_MODE:
                case PduHeaders.CONTENT_CLASS:
                case PduHeaders.RETRIEVE_STATUS:
                case PduHeaders.STORE_STATUS:
                case PduHeaders.RESPONSE_STATUS:
                {
                    int value = extractByteValue(pduDataStream);
                    if (LOCAL_LOGV) {
                        Log.v(LOG_TAG, "parseHeaders: byte: " + headerField + " value: " +
                                value);
                    }

                    try {
                        headers.setOctet(value, headerField);
                    } catch(InvalidHeaderValueException e) {
                        log("Set invalid Octet value: " + value +
                                " into the header filed: " + headerField);
                        return null;
                    } catch(RuntimeException e) {
                        log(headerField + "is not Octet header field!");
                        return null;
                    }
                    break;
                }

                /* Long-Integer */
                case PduHeaders.DATE:
                case PduHeaders.REPLY_CHARGING_SIZE:
                case PduHeaders.MESSAGE_SIZE:
                {
                    try {
                        long value = parseLongInteger(pduDataStream);
                        if (LOCAL_LOGV) {
                            Log.v(LOG_TAG, "parseHeaders: longint: " + headerField + " value: " +
                                    value);
                        }
                        headers.setLongInteger(value, headerField);
                    } catch(RuntimeException e) {
                        log(headerField + "is not Long-Integer header field!");
                        return null;
                    }
                    break;
                }

                /* Integer-Value */
                case PduHeaders.MESSAGE_COUNT:
                case PduHeaders.START:
                case PduHeaders.LIMIT:
                {
                    try {
                        long value = parseIntegerValue(pduDataStream);
                        if (LOCAL_LOGV) {
                            Log.v(LOG_TAG, "parseHeaders: int: " + headerField + " value: " +
                                    value);
                        }
                        headers.setLongInteger(value, headerField);
                    } catch(RuntimeException e) {
                        log(headerField + "is not Long-Integer header field!");
                        return null;
                    }
                    break;
                }

                /* Text-String */
                case PduHeaders.TRANSACTION_ID:
                case PduHeaders.REPLY_CHARGING_ID:
                case PduHeaders.AUX_APPLIC_ID:
                case PduHeaders.APPLIC_ID:
                case PduHeaders.REPLY_APPLIC_ID:
                case PduHeaders.MESSAGE_ID:
                case PduHeaders.REPLACE_ID:
                case PduHeaders.CANCEL_ID:
                case PduHeaders.CONTENT_LOCATION:
                {
                    byte[] value = parseWapString(pduDataStream, TYPE_TEXT_STRING);
                    if (null != value) {
                        try {
                            if (LOCAL_LOGV) {
                                Log.v(LOG_TAG, "parseHeaders: string: " + headerField + " value: " +
                                        new String(value));
                            }
                            headers.setTextString(value, headerField);
                        } catch(NullPointerException e) {
                            log("null pointer error!");
                        } catch(RuntimeException e) {
                            log(headerField + "is not Text-String header field!");
                            return null;
                        }
                    }
                    break;
                }

                /* Encoded-string-value */
                case PduHeaders.SUBJECT:
                case PduHeaders.RECOMMENDED_RETRIEVAL_MODE_TEXT:
                case PduHeaders.RETRIEVE_TEXT:
                case PduHeaders.STATUS_TEXT:
                case PduHeaders.STORE_STATUS_TEXT:
                case PduHeaders.RESPONSE_TEXT:
                {
                    EncodedStringValue value =
                        parseEncodedStringValue(pduDataStream);
                    if (null != value) {
                        try {
                            if (LOCAL_LOGV) {
                                Log.v(LOG_TAG, "parseHeaders: encoded string: " + headerField
                                        + " value: " + value.getString());
                            }
                            headers.setEncodedStringValue(value, headerField);
                        } catch(NullPointerException e) {
                            log("null pointer error!");
                        } catch (RuntimeException e) {
                            log(headerField + "is not Encoded-String-Value header field!");
                            return null;
                        }
                    }
                    break;
                }

                /* Addressing model */
                case PduHeaders.BCC:
                case PduHeaders.CC:
                case PduHeaders.TO:
                {
                    EncodedStringValue value =
                        parseEncodedStringValue(pduDataStream);
                    if (null != value) {
                        byte[] address = value.getTextString();
                        if (null != address) {
                            String str = new String(address);
                            if (LOCAL_LOGV) {
                                Log.v(LOG_TAG, "parseHeaders: (to/cc/bcc) address: " + headerField
                                        + " value: " + str);
                            }
                            int endIndex = str.indexOf("/");
                            if (endIndex > 0) {
                                str = str.substring(0, endIndex);
                            }
                            try {
                                value.setTextString(str.getBytes());
                            } catch(NullPointerException e) {
                                log("null pointer error!");
                                return null;
                            }
                        }

                        try {
                            headers.appendEncodedStringValue(value, headerField);
                        } catch(NullPointerException e) {
                            log("null pointer error!");
                        } catch(RuntimeException e) {
                            log(headerField + "is not Encoded-String-Value header field!");
                            return null;
                        }
                    }
                    break;
                }


                case PduHeaders.DELIVERY_TIME:
                case PduHeaders.EXPIRY:
                case PduHeaders.REPLY_CHARGING_DEADLINE:
                {
                    /* parse Value-length */
                    parseValueLength(pduDataStream);

                    /* Absolute-token or Relative-token */
                    int token = extractByteValue(pduDataStream);

                    /* Date-value or Delta-seconds-value */
                    long timeValue;
                    try {
                        timeValue = parseLongInteger(pduDataStream);
                    } catch(RuntimeException e) {
                        log(headerField + "is not Long-Integer header field!");
                        return null;
                    }
                    if (PduHeaders.VALUE_RELATIVE_TOKEN == token) {
                        /* need to convert the Delta-seconds-value
                         * into Date-value */
                        timeValue = System.currentTimeMillis()/1000 + timeValue;
                    }

                    try {
                        if (LOCAL_LOGV) {
                            Log.v(LOG_TAG, "parseHeaders: time value: " + headerField
                                    + " value: " + timeValue);
                        }
                        headers.setLongInteger(timeValue, headerField);
                    } catch(RuntimeException e) {
                        log(headerField + "is not Long-Integer header field!");
                        return null;
                    }
                    break;
                }

                case PduHeaders.FROM: {
                    EncodedStringValue from = null;
                    parseValueLength(pduDataStream); /* parse value-length */

                    int fromToken = extractByteValue(pduDataStream);

                    if (PduHeaders.FROM_ADDRESS_PRESENT_TOKEN == fromToken) {
                        /* Encoded-string-value */
                        from = parseEncodedStringValue(pduDataStream);
                        if (null != from) {
                            byte[] address = from.getTextString();
                            if (null != address) {
                                String str = new String(address);
                                int endIndex = str.indexOf("/");
                                if (endIndex > 0) {
                                    str = str.substring(0, endIndex);
                                }
                                try {
                                    from.setTextString(str.getBytes());
                                } catch(NullPointerException e) {
                                    log("null pointer error!");
                                    return null;
                                }
                            }
                        }
                    } else {
                        try {
                            from = new EncodedStringValue(
                                    PduHeaders.FROM_INSERT_ADDRESS_TOKEN_STR.getBytes());
                        } catch(NullPointerException e) {
                            log(headerField + "is not Encoded-String-Value header field!");
                            return null;
                        }
                    }

                    try {
                        if (LOCAL_LOGV) {
                            Log.v(LOG_TAG, "parseHeaders: from address: " + headerField
                                    + " value: " + from.getString());
                        }
                        headers.setEncodedStringValue(from, PduHeaders.FROM);
                    } catch(NullPointerException e) {
                        log("null pointer error!");
                    } catch(RuntimeException e) {
                        log(headerField + "is not Encoded-String-Value header field!");
                        return null;
                    }
                    break;
                }

                case PduHeaders.MESSAGE_CLASS: {
                    /* Message-class-value = Class-identifier | Token-text */
                    pduDataStream.mark(1);
                    int messageClass = extractByteValue(pduDataStream);
                    if (LOCAL_LOGV) {
                        Log.v(LOG_TAG, "parseHeaders: MESSAGE_CLASS: " + headerField
                                + " value: " + messageClass);
                    }

                    if (messageClass >= PduHeaders.MESSAGE_CLASS_PERSONAL) {
                        /* Class-identifier */
                        try {
                            if (PduHeaders.MESSAGE_CLASS_PERSONAL == messageClass) {
                                headers.setTextString(
                                        PduHeaders.MESSAGE_CLASS_PERSONAL_STR.getBytes(),
                                        PduHeaders.MESSAGE_CLASS);
                            } else if (PduHeaders.MESSAGE_CLASS_ADVERTISEMENT == messageClass) {
                                headers.setTextString(
                                        PduHeaders.MESSAGE_CLASS_ADVERTISEMENT_STR.getBytes(),
                                        PduHeaders.MESSAGE_CLASS);
                            } else if (PduHeaders.MESSAGE_CLASS_INFORMATIONAL == messageClass) {
                                headers.setTextString(
                                        PduHeaders.MESSAGE_CLASS_INFORMATIONAL_STR.getBytes(),
                                        PduHeaders.MESSAGE_CLASS);
                            } else if (PduHeaders.MESSAGE_CLASS_AUTO == messageClass) {
                                headers.setTextString(
                                        PduHeaders.MESSAGE_CLASS_AUTO_STR.getBytes(),
                                        PduHeaders.MESSAGE_CLASS);
                            }
                        } catch(NullPointerException e) {
                            log("null pointer error!");
                        } catch(RuntimeException e) {
                            log(headerField + "is not Text-String header field!");
                            return null;
                        }
                    } else {
                        /* Token-text */
                        pduDataStream.reset();
                        byte[] messageClassString = parseWapString(pduDataStream, TYPE_TEXT_STRING);
                        if (null != messageClassString) {
                            try {
                                headers.setTextString(messageClassString, PduHeaders.MESSAGE_CLASS);
                            } catch(NullPointerException e) {
                                log("null pointer error!");
                            } catch(RuntimeException e) {
                                log(headerField + "is not Text-String header field!");
                                return null;
                            }
                        }
                    }
                    break;
                }

                case PduHeaders.MMS_VERSION: {
                    int version = parseShortInteger(pduDataStream);

                    try {
                        if (LOCAL_LOGV) {
                            Log.v(LOG_TAG, "parseHeaders: MMS_VERSION: " + headerField
                                    + " value: " + version);
                        }
                        headers.setOctet(version, PduHeaders.MMS_VERSION);
                    } catch(InvalidHeaderValueException e) {
                        log("Set invalid Octet value: " + version +
                                " into the header filed: " + headerField);
                        return null;
                    } catch(RuntimeException e) {
                        log(headerField + "is not Octet header field!");
                        return null;
                    }
                    break;
                }

                case PduHeaders.PREVIOUSLY_SENT_BY: {
                    parseValueLength(pduDataStream);

                    /* parse Forwarded-count-value */
                    try {
                        parseIntegerValue(pduDataStream);
                    } catch(RuntimeException e) {
                        log(headerField + " is not Integer-Value");
                        return null;
                    }

                    /* parse Encoded-string-value */
                    EncodedStringValue previouslySentBy =
                        parseEncodedStringValue(pduDataStream);
                    if (null != previouslySentBy) {
                        try {
                            if (LOCAL_LOGV) {
                                Log.v(LOG_TAG, "parseHeaders: PREVIOUSLY_SENT_BY: " + headerField
                                        + " value: " + previouslySentBy.getString());
                            }
                            headers.setEncodedStringValue(previouslySentBy,
                                    PduHeaders.PREVIOUSLY_SENT_BY);
                        } catch(NullPointerException e) {
                            log("null pointer error!");
                        } catch(RuntimeException e) {
                            log(headerField + "is not Encoded-String-Value header field!");
                            return null;
                        }
                    }
                    break;
                }

                case PduHeaders.PREVIOUSLY_SENT_DATE: {
                    /* Previously-sent-date-value =
                     * Value-length Forwarded-count-value Date-value */
                    /* parse value-length */
                    parseValueLength(pduDataStream);

                    /* parse Forwarded-count-value */
                    try {
                        parseIntegerValue(pduDataStream);
                    } catch(RuntimeException e) {
                        log(headerField + " is not Integer-Value");
                        return null;
                    }

                    /* Date-value */
                    try {
                        long perviouslySentDate = parseLongInteger(pduDataStream);
                        if (LOCAL_LOGV) {
                            Log.v(LOG_TAG, "parseHeaders: PREVIOUSLY_SENT_DATE: " + headerField
                                    + " value: " + perviouslySentDate);
                        }
                        headers.setLongInteger(perviouslySentDate,
                                PduHeaders.PREVIOUSLY_SENT_DATE);
                    } catch(RuntimeException e) {
                        log(headerField + "is not Long-Integer header field!");
                        return null;
                    }
                    break;
                }

                case PduHeaders.MM_FLAGS: {
                    if (LOCAL_LOGV) {
                        Log.v(LOG_TAG, "parseHeaders: MM_FLAGS: " + headerField
                                + " NOT REALLY SUPPORTED");
                    }

                    parseValueLength(pduDataStream);

                    extractByteValue(pduDataStream);

                    parseEncodedStringValue(pduDataStream);

                    break;
                }

                case PduHeaders.MBOX_TOTALS:
                case PduHeaders.MBOX_QUOTAS:
                {
                    if (LOCAL_LOGV) {
                        Log.v(LOG_TAG, "parseHeaders: MBOX_TOTALS: " + headerField);
                    }
                    parseValueLength(pduDataStream);

                    extractByteValue(pduDataStream);

                    try {
                        parseIntegerValue(pduDataStream);
                    } catch(RuntimeException e) {
                        log(headerField + " is not Integer-Value");
                        return null;
                    }

                    break;
                }

                case PduHeaders.ELEMENT_DESCRIPTOR: {
                    if (LOCAL_LOGV) {
                        Log.v(LOG_TAG, "parseHeaders: ELEMENT_DESCRIPTOR: " + headerField);
                    }
                    parseContentType(pduDataStream, null);

                    break;
                }

                case PduHeaders.CONTENT_TYPE: {
                    HashMap<Integer, Object> map =
                        new HashMap<Integer, Object>();
                    byte[] contentType =
                        parseContentType(pduDataStream, map);

                    if (null != contentType) {
                        try {
                            if (LOCAL_LOGV) {
                                Log.v(LOG_TAG, "parseHeaders: CONTENT_TYPE: " + headerField +
                                        contentType.toString());
                            }
                            headers.setTextString(contentType, PduHeaders.CONTENT_TYPE);
                        } catch(NullPointerException e) {
                            log("null pointer error!");
                        } catch(RuntimeException e) {
                            log(headerField + "is not Text-String header field!");
                            return null;
                        }
                    }

                    mStartParam = (byte[]) map.get(PduPart.P_START);

                    mTypeParam= (byte[]) map.get(PduPart.P_TYPE);

                    keepParsing = false;
                    break;
                }

                case PduHeaders.CONTENT:
                case PduHeaders.ADDITIONAL_HEADERS:
                case PduHeaders.ATTRIBUTES:
                default: {
                    if (LOCAL_LOGV) {
                        Log.v(LOG_TAG, "parseHeaders: Unknown header: " + headerField);
                    }
                    log("Unknown header");
                }
            }
        }

        return headers;
    }

    protected PduBody parseParts(ByteArrayInputStream pduDataStream) {
        if (pduDataStream == null) {
            return null;
        }

        int count = parseUnsignedInt(pduDataStream); // get the number of parts
        PduBody body = new PduBody();

        for (int i = 0 ; i < count ; i++) {
            int headerLength = parseUnsignedInt(pduDataStream);
            int dataLength = parseUnsignedInt(pduDataStream);
            PduPart part = new PduPart();
            int startPos = pduDataStream.available();
            if (startPos <= 0) {
                // Invalid part.
                return null;
            }

            /* parse part's content-type */
            HashMap<Integer, Object> map = new HashMap<Integer, Object>();
            byte[] contentType = parseContentType(pduDataStream, map);
            if (null != contentType) {
                part.setContentType(contentType);
            } else {
                part.setContentType((PduContentTypes.contentTypes[0]).getBytes()); //"*/*"
            }

            /* get name parameter */
            byte[] name = (byte[]) map.get(PduPart.P_NAME);
            if (null != name) {
                part.setName(name);
            }

            /* get charset parameter */
            Integer charset = (Integer) map.get(PduPart.P_CHARSET);
            if (null != charset) {
                part.setCharset(charset);
            }

            /* parse part's headers */
            int endPos = pduDataStream.available();
            int partHeaderLen = headerLength - (startPos - endPos);
            if (partHeaderLen > 0) {
                if (false == parsePartHeaders(pduDataStream, part, partHeaderLen)) {
                    // Parse part header faild.
                    return null;
                }
            } else if (partHeaderLen < 0) {
                // Invalid length of content-type.
                return null;
            }


            if ((null == part.getContentLocation())
                    && (null == part.getName())
                    && (null == part.getFilename())
                    && (null == part.getContentId())) {
                part.setContentLocation(Long.toOctalString(
                        System.currentTimeMillis()).getBytes());
            }

            /* get part's data */
            if (dataLength > 0) {
                byte[] partData = new byte[dataLength];
                String partContentType = new String(part.getContentType());
                pduDataStream.read(partData, 0, dataLength);
                if (partContentType.equalsIgnoreCase(ContentType.MULTIPART_ALTERNATIVE)) {
                    // parse "multipart/vnd.wap.multipart.alternative".
                    PduBody childBody = parseParts(new ByteArrayInputStream(partData));
                    // take the first part of children.
                    part = childBody.getPart(0);
                } else {
                    // Check Content-Transfer-Encoding.
                    byte[] partDataEncoding = part.getContentTransferEncoding();
                    if (null != partDataEncoding) {
                        String encoding = new String(partDataEncoding);
                        if (encoding.equalsIgnoreCase(PduPart.P_BASE64)) {
                            // Decode "base64" into "binary".
                            partData = Base64.decodeBase64(partData);
                        } else if (encoding.equalsIgnoreCase(PduPart.P_QUOTED_PRINTABLE)) {
                            // Decode "quoted-printable" into "binary".
                            partData = QuotedPrintable.decodeQuotedPrintable(partData);
                        } else {
                            // "binary" is the default encoding.
                        }
                    }
                    if (null == partData) {
                        log("Decode part data error!");
                        return null;
                    }
                    part.setData(partData);
                }
            }

            /* add this part to body */
            if (THE_FIRST_PART == checkPartPosition(part)) {
                /* this is the first part */
                body.addPart(0, part);
            } else {
                /* add the part to the end */
                body.addPart(part);
            }
        }

        return body;
    }

    private static void log(String text) {
        if (LOCAL_LOGV) {
            Log.v(LOG_TAG, text);
        }
    }

    protected static int parseUnsignedInt(ByteArrayInputStream pduDataStream) {
        assert(null != pduDataStream);
        int result = 0;
        int temp = pduDataStream.read();
        if (temp == -1) {
            return temp;
        }

        while((temp & 0x80) != 0) {
            result = result << 7;
            result |= temp & 0x7F;
            temp = pduDataStream.read();
            if (temp == -1) {
                return temp;
            }
        }

        result = result << 7;
        result |= temp & 0x7F;

        return result;
    }

    protected static int parseValueLength(ByteArrayInputStream pduDataStream) {

        assert(null != pduDataStream);
        int temp = pduDataStream.read();
        assert(-1 != temp);
        int first = temp & 0xFF;

        if (first <= SHORT_LENGTH_MAX) {
            return first;
        } else if (first == LENGTH_QUOTE) {
            return parseUnsignedInt(pduDataStream);
        }

        throw new RuntimeException ("Value length > LENGTH_QUOTE!");
    }


    protected static EncodedStringValue parseEncodedStringValue(ByteArrayInputStream pduDataStream){
        assert(null != pduDataStream);
        pduDataStream.mark(1);
        EncodedStringValue returnValue = null;
        int charset = 0;
        int temp = pduDataStream.read();
        assert(-1 != temp);
        int first = temp & 0xFF;
        if (first == 0) {
            return new EncodedStringValue("");
        }

        pduDataStream.reset();
        if (first < TEXT_MIN) {
            parseValueLength(pduDataStream);

            charset = parseShortInteger(pduDataStream); //get the "Charset"
        }

        byte[] textString = parseWapString(pduDataStream, TYPE_TEXT_STRING);

        try {
            if (0 != charset) {
                returnValue = new EncodedStringValue(charset, textString);
            } else {
                returnValue = new EncodedStringValue(textString);
            }
        } catch(Exception e) {
            return null;
        }

        return returnValue;
    }

    protected static byte[] parseWapString(ByteArrayInputStream pduDataStream,
            int stringType) {
        assert(null != pduDataStream);

        // Mark supposed beginning of Text-string
        // We will have to mark again if first char is QUOTE or QUOTED_STRING_FLAG
        pduDataStream.mark(1);

        // Check first char
        int temp = pduDataStream.read();
        assert(-1 != temp);
        if ((TYPE_QUOTED_STRING == stringType) &&
                (QUOTED_STRING_FLAG == temp)) {
            // Mark again if QUOTED_STRING_FLAG and ignore it
            pduDataStream.mark(1);
        } else if ((TYPE_TEXT_STRING == stringType) &&
                (QUOTE == temp)) {
            // Mark again if QUOTE and ignore it
            pduDataStream.mark(1);
        } else {
            // Otherwise go back to origin
            pduDataStream.reset();
        }

        // We are now definitely at the beginning of string
        return getWapString(pduDataStream, stringType);
    }


    protected static boolean isTokenCharacter(int ch) {
        if((ch < 33) || (ch > 126)) {
            return false;
        }

        switch(ch) {
            case '"': /* '"' */
            case '(': /* '(' */
            case ')': /* ')' */
            case ',': /* ',' */
            case '/': /* '/' */
            case ':': /* ':' */
            case ';': /* ';' */
            case '<': /* '<' */
            case '=': /* '=' */
            case '>': /* '>' */
            case '?': /* '?' */
            case '@': /* '@' */
            case '[': /* '[' */
            case '\\': /* '\' */
            case ']': /* ']' */
            case '{': /* '{' */
            case '}': /* '}' */
                return false;
        }

        return true;
    }


    protected static boolean isText(int ch) {
        if(((ch >= 32) && (ch <= 126)) || ((ch >= 128) && (ch <= 255))) {
            return true;
        }

        switch(ch) {
            case '\t': /* '\t' */
            case '\n': /* '\n' */
            case '\r': /* '\r' */
                return true;
        }

        return false;
    }

    protected static byte[] getWapString(ByteArrayInputStream pduDataStream,
            int stringType) {
        assert(null != pduDataStream);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int temp = pduDataStream.read();
        assert(-1 != temp);
        while((-1 != temp) && ('\0' != temp)) {
            // check each of the character
            if (stringType == TYPE_TOKEN_STRING) {
                if (isTokenCharacter(temp)) {
                    out.write(temp);
                }
            } else {
                if (isText(temp)) {
                    out.write(temp);
                }
            }

            temp = pduDataStream.read();
            assert(-1 != temp);
        }

        if (out.size() > 0) {
            return out.toByteArray();
        }

        return null;
    }


    protected static int extractByteValue(ByteArrayInputStream pduDataStream) {
        assert(null != pduDataStream);
        int temp = pduDataStream.read();
        assert(-1 != temp);
        return temp & 0xFF;
    }


    protected static int parseShortInteger(ByteArrayInputStream pduDataStream) {

        assert(null != pduDataStream);
        int temp = pduDataStream.read();
        assert(-1 != temp);
        return temp & 0x7F;
    }


    protected static long parseLongInteger(ByteArrayInputStream pduDataStream) {

        assert(null != pduDataStream);
        int temp = pduDataStream.read();
        assert(-1 != temp);
        int count = temp & 0xFF;

        if (count > LONG_INTEGER_LENGTH_MAX) {
            throw new RuntimeException("Octet count greater than 8 and I can't represent that!");
        }

        long result = 0;

        for (int i = 0 ; i < count ; i++) {
            temp = pduDataStream.read();
            assert(-1 != temp);
            result <<= 8;
            result += (temp & 0xFF);
        }

        return result;
    }


    protected static long parseIntegerValue(ByteArrayInputStream pduDataStream) {

        assert(null != pduDataStream);
        pduDataStream.mark(1);
        int temp = pduDataStream.read();
        assert(-1 != temp);
        pduDataStream.reset();
        if (temp > SHORT_INTEGER_MAX) {
            return parseShortInteger(pduDataStream);
        } else {
            return parseLongInteger(pduDataStream);
        }
    }

    protected static int skipWapValue(ByteArrayInputStream pduDataStream, int length) {
        assert(null != pduDataStream);
        byte[] area = new byte[length];
        int readLen = pduDataStream.read(area, 0, length);
        if (readLen < length) { //The actually read length is lower than the length
            return -1;
        } else {
            return readLen;
        }
    }

    protected static void parseContentTypeParams(ByteArrayInputStream pduDataStream,
            HashMap<Integer, Object> map, Integer length) {

        assert(null != pduDataStream);
        assert(length > 0);

        int startPos = pduDataStream.available();
        int tempPos = 0;
        int lastLen = length;
        while(0 < lastLen) {
            int param = pduDataStream.read();
            assert(-1 != param);
            lastLen--;

            switch (param) {

                case PduPart.P_TYPE:
                case PduPart.P_CT_MR_TYPE:
                    pduDataStream.mark(1);
                    int first = extractByteValue(pduDataStream);
                    pduDataStream.reset();
                    if (first > TEXT_MAX) {
                        // Short-integer (well-known type)
                        int index = parseShortInteger(pduDataStream);

                        if (index < PduContentTypes.contentTypes.length) {
                            byte[] type = (PduContentTypes.contentTypes[index]).getBytes();
                            map.put(PduPart.P_TYPE, type);
                        } else {
                            //not support this type, ignore it.
                        }
                    } else {
                        // Text-String (extension-media)
                        byte[] type = parseWapString(pduDataStream, TYPE_TEXT_STRING);
                        if ((null != type) && (null != map)) {
                            map.put(PduPart.P_TYPE, type);
                        }
                    }

                    tempPos = pduDataStream.available();
                    lastLen = length - (startPos - tempPos);
                    break;


                case PduPart.P_START:
                case PduPart.P_DEP_START:
                    byte[] start = parseWapString(pduDataStream, TYPE_TEXT_STRING);
                    if ((null != start) && (null != map)) {
                        map.put(PduPart.P_START, start);
                    }

                    tempPos = pduDataStream.available();
                    lastLen = length - (startPos - tempPos);
                    break;


                case PduPart.P_CHARSET:
                    pduDataStream.mark(1);
                    int firstValue = extractByteValue(pduDataStream);
                    pduDataStream.reset();
                    //Check first char
                    if (((firstValue > TEXT_MIN) && (firstValue < TEXT_MAX)) ||
                            (END_STRING_FLAG == firstValue)) {
                        //Text-String (extension-charset)
                        byte[] charsetStr = parseWapString(pduDataStream, TYPE_TEXT_STRING);
                        try {
                            int charsetInt = CharacterSets.getMibEnumValue(
                                    new String(charsetStr));
                            map.put(PduPart.P_CHARSET, charsetInt);
                        } catch (UnsupportedEncodingException e) {
                            // Not a well-known charset, use "*".
                            Log.e(LOG_TAG, Arrays.toString(charsetStr), e);
                            map.put(PduPart.P_CHARSET, CharacterSets.ANY_CHARSET);
                        }
                    } else {
                        //Well-known-charset
                        int charset = (int) parseIntegerValue(pduDataStream);
                        if (map != null) {
                            map.put(PduPart.P_CHARSET, charset);
                        }
                    }

                    tempPos = pduDataStream.available();
                    lastLen = length - (startPos - tempPos);
                    break;


                case PduPart.P_DEP_NAME:
                case PduPart.P_NAME:
                    byte[] name = parseWapString(pduDataStream, TYPE_TEXT_STRING);
                    if ((null != name) && (null != map)) {
                        map.put(PduPart.P_NAME, name);
                    }

                    tempPos = pduDataStream.available();
                    lastLen = length - (startPos - tempPos);
                    break;
                default:
                    if (LOCAL_LOGV) {
                        Log.v(LOG_TAG, "Not supported Content-Type parameter");
                    }
                if (-1 == skipWapValue(pduDataStream, lastLen)) {
                    Log.e(LOG_TAG, "Corrupt Content-Type");
                } else {
                    lastLen = 0;
                }
                break;
            }
        }

        if (0 != lastLen) {
            Log.e(LOG_TAG, "Corrupt Content-Type");
        }
    }

    protected static byte[] parseContentType(ByteArrayInputStream pduDataStream,
            HashMap<Integer, Object> map) {

        assert(null != pduDataStream);

        byte[] contentType = null;
        pduDataStream.mark(1);
        int temp = pduDataStream.read();
        assert(-1 != temp);
        pduDataStream.reset();

        int cur = (temp & 0xFF);

        if (cur < TEXT_MIN) {
            int length = parseValueLength(pduDataStream);
            int startPos = pduDataStream.available();
            pduDataStream.mark(1);
            temp = pduDataStream.read();
            assert(-1 != temp);
            pduDataStream.reset();
            int first = (temp & 0xFF);

            if ((first >= TEXT_MIN) && (first <= TEXT_MAX)) {
                contentType = parseWapString(pduDataStream, TYPE_TEXT_STRING);
            } else if (first > TEXT_MAX) {
                int index = parseShortInteger(pduDataStream);

                if (index < PduContentTypes.contentTypes.length) { //well-known type
                    contentType = (PduContentTypes.contentTypes[index]).getBytes();
                } else {
                    pduDataStream.reset();
                    contentType = parseWapString(pduDataStream, TYPE_TEXT_STRING);
                }
            } else {
                Log.e(LOG_TAG, "Corrupt content-type");
                return (PduContentTypes.contentTypes[0]).getBytes(); //"*/*"
            }

            int endPos = pduDataStream.available();
            int parameterLen = length - (startPos - endPos);
            if (parameterLen > 0) {//have parameters
                parseContentTypeParams(pduDataStream, map, parameterLen);
            }

            if (parameterLen < 0) {
                Log.e(LOG_TAG, "Corrupt MMS message");
                return (PduContentTypes.contentTypes[0]).getBytes(); //"*/*"
            }
        } else if (cur <= TEXT_MAX) {
            contentType = parseWapString(pduDataStream, TYPE_TEXT_STRING);
        } else {
            contentType =
                (PduContentTypes.contentTypes[parseShortInteger(pduDataStream)]).getBytes();
        }

        return contentType;
    }

    protected boolean parsePartHeaders(ByteArrayInputStream pduDataStream,
            PduPart part, int length) {
        assert(null != pduDataStream);
        assert(null != part);
        assert(length > 0);


        int startPos = pduDataStream.available();
        int tempPos = 0;
        int lastLen = length;
        while(0 < lastLen) {
            int header = pduDataStream.read();
            assert(-1 != header);
            lastLen--;

            if (header > TEXT_MAX) {
                // Number assigned headers.
                switch (header) {
                    case PduPart.P_CONTENT_LOCATION:

                        byte[] contentLocation = parseWapString(pduDataStream, TYPE_TEXT_STRING);
                        if (null != contentLocation) {
                            part.setContentLocation(contentLocation);
                        }

                        tempPos = pduDataStream.available();
                        lastLen = length - (startPos - tempPos);
                        break;
                    case PduPart.P_CONTENT_ID:

                        byte[] contentId = parseWapString(pduDataStream, TYPE_QUOTED_STRING);
                        if (null != contentId) {
                            part.setContentId(contentId);
                        }

                        tempPos = pduDataStream.available();
                        lastLen = length - (startPos - tempPos);
                        break;
                    case PduPart.P_DEP_CONTENT_DISPOSITION:
                    case PduPart.P_CONTENT_DISPOSITION:

                        if (mParseContentDisposition) {
                            int len = parseValueLength(pduDataStream);
                            pduDataStream.mark(1);
                            int thisStartPos = pduDataStream.available();
                            int thisEndPos = 0;
                            int value = pduDataStream.read();

                            if (value == PduPart.P_DISPOSITION_FROM_DATA ) {
                                part.setContentDisposition(PduPart.DISPOSITION_FROM_DATA);
                            } else if (value == PduPart.P_DISPOSITION_ATTACHMENT) {
                                part.setContentDisposition(PduPart.DISPOSITION_ATTACHMENT);
                            } else if (value == PduPart.P_DISPOSITION_INLINE) {
                                part.setContentDisposition(PduPart.DISPOSITION_INLINE);
                            } else {
                                pduDataStream.reset();
                                /* Token-text */
                                part.setContentDisposition(parseWapString(pduDataStream
                                        , TYPE_TEXT_STRING));
                            }

                            /* get filename parameter and skip other parameters */
                            thisEndPos = pduDataStream.available();
                            if (thisStartPos - thisEndPos < len) {
                                value = pduDataStream.read();
                                if (value == PduPart.P_FILENAME) { //filename is text-string
                                    part.setFilename(parseWapString(pduDataStream
                                            , TYPE_TEXT_STRING));
                                }

                                /* skip other parameters */
                                thisEndPos = pduDataStream.available();
                                if (thisStartPos - thisEndPos < len) {
                                    int last = len - (thisStartPos - thisEndPos);
                                    byte[] temp = new byte[last];
                                    pduDataStream.read(temp, 0, last);
                                }
                            }

                            tempPos = pduDataStream.available();
                            lastLen = length - (startPos - tempPos);
                        }
                        break;
                    default:
                        if (LOCAL_LOGV) {
                            Log.v(LOG_TAG, "Not supported Part headers: " + header);
                        }
                    if (-1 == skipWapValue(pduDataStream, lastLen)) {
                        Log.e(LOG_TAG, "Corrupt Part headers");
                        return false;
                    }
                    lastLen = 0;
                    break;
                }
            } else if ((header >= TEXT_MIN) && (header <= TEXT_MAX)) {
                // Not assigned header.
                byte[] tempHeader = parseWapString(pduDataStream, TYPE_TEXT_STRING);
                byte[] tempValue = parseWapString(pduDataStream, TYPE_TEXT_STRING);

                // Check the header whether it is "Content-Transfer-Encoding".
                if (true ==
                    PduPart.CONTENT_TRANSFER_ENCODING.equalsIgnoreCase(new String(tempHeader))) {
                    part.setContentTransferEncoding(tempValue);
                }

                tempPos = pduDataStream.available();
                lastLen = length - (startPos - tempPos);
            } else {
                if (LOCAL_LOGV) {
                    Log.v(LOG_TAG, "Not supported Part headers: " + header);
                }
                // Skip all headers of this part.
                if (-1 == skipWapValue(pduDataStream, lastLen)) {
                    Log.e(LOG_TAG, "Corrupt Part headers");
                    return false;
                }
                lastLen = 0;
            }
        }

        if (0 != lastLen) {
            Log.e(LOG_TAG, "Corrupt Part headers");
            return false;
        }

        return true;
    }

    private static int checkPartPosition(PduPart part) {
        assert(null != part);
        if ((null == mTypeParam) &&
                (null == mStartParam)) {
            return THE_LAST_PART;
        }

        /* check part's content-id */
        if (null != mStartParam) {
            byte[] contentId = part.getContentId();
            if (null != contentId) {
                if (true == Arrays.equals(mStartParam, contentId)) {
                    return THE_FIRST_PART;
                }
            }
        }

        /* check part's content-type */
        if (null != mTypeParam) {
            byte[] contentType = part.getContentType();
            if (null != contentType) {
                if (true == Arrays.equals(mTypeParam, contentType)) {
                    return THE_FIRST_PART;
                }
            }
        }

        return THE_LAST_PART;
    }

    protected static boolean checkMandatoryHeader(PduHeaders headers) {
        if (null == headers) {
            return false;
        }

        /* get message type */
        int messageType = headers.getOctet(PduHeaders.MESSAGE_TYPE);

        /* check Mms-Version field */
        int mmsVersion = headers.getOctet(PduHeaders.MMS_VERSION);
        if (0 == mmsVersion) {
            // Every message should have Mms-Version field.
            return false;
        }

        /* check mandatory header fields */
        switch (messageType) {
            case PduHeaders.MESSAGE_TYPE_SEND_REQ:
                // Content-Type field.
                byte[] srContentType = headers.getTextString(PduHeaders.CONTENT_TYPE);
                if (null == srContentType) {
                    return false;
                }

                // From field.
                EncodedStringValue srFrom = headers.getEncodedStringValue(PduHeaders.FROM);
                if (null == srFrom) {
                    return false;
                }

                // Transaction-Id field.
                byte[] srTransactionId = headers.getTextString(PduHeaders.TRANSACTION_ID);
                if (null == srTransactionId) {
                    return false;
                }

                break;
            case PduHeaders.MESSAGE_TYPE_SEND_CONF:
                // Response-Status field.
                int scResponseStatus = headers.getOctet(PduHeaders.RESPONSE_STATUS);
                if (0 == scResponseStatus) {
                    return false;
                }

                // Transaction-Id field.
                byte[] scTransactionId = headers.getTextString(PduHeaders.TRANSACTION_ID);
                if (null == scTransactionId) {
                    return false;
                }

                break;
            case PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND:
                // Content-Location field.
                byte[] niContentLocation = headers.getTextString(PduHeaders.CONTENT_LOCATION);
                if (null == niContentLocation) {
                    return false;
                }

                // Expiry field.
                long niExpiry = headers.getLongInteger(PduHeaders.EXPIRY);
                if (-1 == niExpiry) {
                    return false;
                }

                // Message-Class field.
                byte[] niMessageClass = headers.getTextString(PduHeaders.MESSAGE_CLASS);
                if (null == niMessageClass) {
                    return false;
                }

                // Message-Size field.
                long niMessageSize = headers.getLongInteger(PduHeaders.MESSAGE_SIZE);
                if (-1 == niMessageSize) {
                    return false;
                }

                // Transaction-Id field.
                byte[] niTransactionId = headers.getTextString(PduHeaders.TRANSACTION_ID);
                if (null == niTransactionId) {
                    return false;
                }

                break;
            case PduHeaders.MESSAGE_TYPE_NOTIFYRESP_IND:
                // Status field.
                int nriStatus = headers.getOctet(PduHeaders.STATUS);
                if (0 == nriStatus) {
                    return false;
                }

                // Transaction-Id field.
                byte[] nriTransactionId = headers.getTextString(PduHeaders.TRANSACTION_ID);
                if (null == nriTransactionId) {
                    return false;
                }

                break;
            case PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF:
                // Content-Type field.
                byte[] rcContentType = headers.getTextString(PduHeaders.CONTENT_TYPE);
                if (null == rcContentType) {
                    return false;
                }

                // Date field.
                long rcDate = headers.getLongInteger(PduHeaders.DATE);
                if (-1 == rcDate) {
                    return false;
                }

                break;
            case PduHeaders.MESSAGE_TYPE_DELIVERY_IND:
                // Date field.
                long diDate = headers.getLongInteger(PduHeaders.DATE);
                if (-1 == diDate) {
                    return false;
                }

                // Message-Id field.
                byte[] diMessageId = headers.getTextString(PduHeaders.MESSAGE_ID);
                if (null == diMessageId) {
                    return false;
                }

                // Status field.
                int diStatus = headers.getOctet(PduHeaders.STATUS);
                if (0 == diStatus) {
                    return false;
                }

                // To field.
                EncodedStringValue[] diTo = headers.getEncodedStringValues(PduHeaders.TO);
                if (null == diTo) {
                    return false;
                }

                break;
            case PduHeaders.MESSAGE_TYPE_ACKNOWLEDGE_IND:
                // Transaction-Id field.
                byte[] aiTransactionId = headers.getTextString(PduHeaders.TRANSACTION_ID);
                if (null == aiTransactionId) {
                    return false;
                }

                break;
            case PduHeaders.MESSAGE_TYPE_READ_ORIG_IND:
                // Date field.
                long roDate = headers.getLongInteger(PduHeaders.DATE);
                if (-1 == roDate) {
                    return false;
                }

                // From field.
                EncodedStringValue roFrom = headers.getEncodedStringValue(PduHeaders.FROM);
                if (null == roFrom) {
                    return false;
                }

                // Message-Id field.
                byte[] roMessageId = headers.getTextString(PduHeaders.MESSAGE_ID);
                if (null == roMessageId) {
                    return false;
                }

                // Read-Status field.
                int roReadStatus = headers.getOctet(PduHeaders.READ_STATUS);
                if (0 == roReadStatus) {
                    return false;
                }

                // To field.
                EncodedStringValue[] roTo = headers.getEncodedStringValues(PduHeaders.TO);
                if (null == roTo) {
                    return false;
                }

                break;
            case PduHeaders.MESSAGE_TYPE_READ_REC_IND:
                // From field.
                EncodedStringValue rrFrom = headers.getEncodedStringValue(PduHeaders.FROM);
                if (null == rrFrom) {
                    return false;
                }

                // Message-Id field.
                byte[] rrMessageId = headers.getTextString(PduHeaders.MESSAGE_ID);
                if (null == rrMessageId) {
                    return false;
                }

                // Read-Status field.
                int rrReadStatus = headers.getOctet(PduHeaders.READ_STATUS);
                if (0 == rrReadStatus) {
                    return false;
                }

                // To field.
                EncodedStringValue[] rrTo = headers.getEncodedStringValues(PduHeaders.TO);
                if (null == rrTo) {
                    return false;
                }

                break;
            default:
                // Parser doesn't support this message type in this version.
                return false;
        }

        return true;
    }
}
