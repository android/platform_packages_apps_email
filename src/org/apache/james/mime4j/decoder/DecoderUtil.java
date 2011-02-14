/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mime4j.decoder;

//BEGIN android-changed: Stubbing out logging
import org.apache.james.mime4j.Log;
import org.apache.james.mime4j.LogFactory;
//END android-changed
import org.apache.james.mime4j.util.CharsetUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * import new packages to detect charset
 * Block Begin
 * @author shinwook
 * @date 2011.02.10.
 */
import java.io.*;
import java.net.*;
import java.util.*;
import org.mozilla.intl.chardet.*;
// Block End

/**

 * Static methods for decoding strings, byte arrays and encoded words.
 *
 * 
 * @version $Id: DecoderUtil.java,v 1.3 2005/02/07 15:33:59 ntherning Exp $
 */
public class DecoderUtil {
    private static Log log = LogFactory.getLog(DecoderUtil.class);
    
    /**
     * Using mozilla chardet
     *
     * @author shinwook
     * @date 2011.02.10.
     * @param s the string to detect
     * @return the charset the string might use (estimated result)
     * 			return null if it is US-ASCII (decoding is not needed)
     */
    public static String chardet(String s)  {
        if (s == null)
            return s;

        // Initalize the nsDetector() with default locale Kr (5) or all (nsPSMDetector.ALL)
        int lang = nsPSMDetector.ALL;
        nsDetector det = new nsDetector(lang) ;

        // Set an observer...
        // The Notify() will be called when a matching charset is found.
        det.Init(new nsICharsetDetectionObserver() {
            public void Notify(String charset) {
                log.info("Detected CHARSET = " + charset);
            }
        });

        String result;

        ByteArrayOutputStream imp = new ByteArrayOutputStream();

        char[] chars = s.toCharArray();
        for (char c : chars)
            imp.write(c);

	byte[] buf = imp.toByteArray();
	boolean isAscii = true;

	// Check if the stream is only ascii.
	isAscii = det.isAscii(buf, buf.length);
    // DoIt if non-ascii and not done yet.
    if (!isAscii) {
        det.DoIt(buf, buf.length, false);
    }
    det.DataEnd();

       result = null;
        if (isAscii) {
            // result = new String("US-ASCII");
            result = null;
        } else  {
            String prob[] = det.getProbableCharsets() ;

            // case Korean.
            for (String charset : prob)  {
                if (charset.equals("EUC-KR")){
                    result = String.valueOf("EUC-KR"); //new String("EUC-KR"); //sec.email tom.jung 20100818 For RSAR 3rd.
                    break;
                }
            }
        }
        return result;
    }
    /**
     * Check if it is encoded words and unless it is, decode it with euc-kr
     *
     * @author shinwook
     * @date 2011.02.10.
     * @param s the string to decode
     * @return the decoded bytes
     */
    public static String justDecode(String s, String charset)  {
        // if it is encoded words, just return as it was.
        if (s == null || s.indexOf("=?") != -1)  {
            return s;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String result;
        try {
            char[] chars = s.toCharArray();

            for (char c : chars)  {
                baos.write(c);
            }
            result = new String(baos.toByteArray(), CharsetUtil.toJavaCharset(charset.toLowerCase()));
        }
            catch(UnsupportedEncodingException e)  {
            result = s;
        }

        return result;
    }

    /**
     * Check if the string is an encoded-words, then decode it
     * if it is not an encoded-words, check if it is encoded
     * if it is, justDecode and return the result
     *
     * @author shinwook
     * @date 2011.02.10.
     * @param s the string to check and decode
     * @return the decoded string
     */
    public static String decodeGeneric(String s)  {
        if (s == null)
            return s;

        String result;
        if (s.indexOf("=?") == -1)  {
            // detect
            String charset = chardet(s);
            if (charset == null)
                result = s;
            else
                result = justDecode(s, charset);
        }
        else  {
            result = decodeEncodedWords(s);
        }

        return result;
    }
    /**
     * Decodes a string containing quoted-printable encoded data. 
     * 
     * @param s the string to decode.
     * @return the decoded bytes.
     */
    public static byte[] decodeBaseQuotedPrintable(String s) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            byte[] bytes = s.getBytes("US-ASCII");
            
            QuotedPrintableInputStream is = new QuotedPrintableInputStream(
                                               new ByteArrayInputStream(bytes));
            
            int b = 0;
            while ((b = is.read()) != -1) {
                baos.write(b);
            }
        } catch (IOException e) {
            /*
             * This should never happen!
             */
            log.error(e);
        }
        
        return baos.toByteArray();
    }

    /**
     * Decodes a string containing base64 encoded data. 
     * 
     * @param s the string to decode.
     * @return the decoded bytes.
     */
    public static byte[] decodeBase64(String s) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            byte[] bytes = s.getBytes("US-ASCII");
            
            Base64InputStream is = new Base64InputStream(
                                        new ByteArrayInputStream(bytes));
            
            int b = 0;
            while ((b = is.read()) != -1) {
                baos.write(b);
            }
        } catch (IOException e) {
            /*
             * This should never happen!
             */
            log.error(e);
        }
        
        return baos.toByteArray();
    }
    
    /**
     * Decodes an encoded word encoded with the 'B' encoding (described in 
     * RFC 2047) found in a header field body.
     * 
     * @param encodedWord the encoded word to decode.
     * @param charset the Java charset to use.
     * @return the decoded string.
     * @throws UnsupportedEncodingException if the given Java charset isn't 
     *         supported.
     */
    public static String decodeB(String encodedWord, String charset) 
            throws UnsupportedEncodingException {
        
        return new String(decodeBase64(encodedWord), charset);
    }
    
    /**
     * Decodes an encoded word encoded with the 'Q' encoding (described in 
     * RFC 2047) found in a header field body.
     * 
     * @param encodedWord the encoded word to decode.
     * @param charset the Java charset to use.
     * @return the decoded string.
     * @throws UnsupportedEncodingException if the given Java charset isn't 
     *         supported.
     */
    public static String decodeQ(String encodedWord, String charset)
            throws UnsupportedEncodingException {
           
        /*
         * Replace _ with =20
         */
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < encodedWord.length(); i++) {
            char c = encodedWord.charAt(i);
            if (c == '_') {
                sb.append("=20");
            } else {
                sb.append(c);
            }
        }
        
        return new String(decodeBaseQuotedPrintable(sb.toString()), charset);
    }
    
    /**
     * Decodes a string containing encoded words as defined by RFC 2047.
     * Encoded words in have the form 
     * =?charset?enc?Encoded word?= where enc is either 'Q' or 'q' for 
     * quoted-printable and 'B' or 'b' for Base64.
     * 
     * ANDROID:  COPIED FROM A NEWER VERSION OF MIME4J
     * 
     * @param body the string to decode.
     * @return the decoded string.
     */
    public static String decodeEncodedWords(String body) {
        
        // ANDROID:  Most strings will not include "=?" so a quick test can prevent unneeded
        // object creation.  This could also be handled via lazy creation of the StringBuilder.
        if (body.indexOf("=?") == -1) {
            return body;
        }
        
        int previousEnd = 0;
        boolean previousWasEncoded = false;

        StringBuilder sb = new StringBuilder();

        while (true) {
            int begin = body.indexOf("=?", previousEnd);
            
            // ANDROID:  The mime4j original version has an error here.  It gets confused if
            // the encoded string begins with an '=' (just after "?Q?").  This patch seeks forward
            // to find the two '?' in the "header", before looking for the final "?=".
            int endScan = begin + 2;
            if (begin != -1) {
                int qm1 = body.indexOf('?', endScan + 2);
                int qm2 = body.indexOf('?', qm1 + 1);
                if (qm2 != -1) {
                    endScan = qm2 + 1;
                }
            }
            
            int end = begin == -1 ? -1 : body.indexOf("?=", endScan);
            if (end == -1) {
                if (previousEnd == 0)
                    return body;

                sb.append(body.substring(previousEnd));
                return sb.toString();
            }
            end += 2;

            String sep = body.substring(previousEnd, begin);

            String decoded = decodeEncodedWord(body, begin, end);
            if (decoded == null) {
                sb.append(sep);
                sb.append(body.substring(begin, end));
            } else {
                if (!previousWasEncoded || !CharsetUtil.isWhitespace(sep)) {
                    sb.append(sep);
                }
                sb.append(decoded);
            }

            previousEnd = end;
            previousWasEncoded = decoded != null;
        }
    }

    // return null on error
    private static String decodeEncodedWord(String body, int begin, int end) {
        int qm1 = body.indexOf('?', begin + 2);
        if (qm1 == end - 2)
            return null;

        int qm2 = body.indexOf('?', qm1 + 1);
        if (qm2 == end - 2)
            return null;

        String mimeCharset = body.substring(begin + 2, qm1);
        String encoding = body.substring(qm1 + 1, qm2);
        String encodedText = body.substring(qm2 + 1, end - 2);

        String charset = CharsetUtil.toJavaCharset(mimeCharset);
        if (charset == null) {
            if (log.isWarnEnabled()) {
                log.warn("MIME charset '" + mimeCharset + "' in encoded word '"
                        + body.substring(begin, end) + "' doesn't have a "
                        + "corresponding Java charset");
            }
            return null;
        } else if (!CharsetUtil.isDecodingSupported(charset)) {
            if (log.isWarnEnabled()) {
                log.warn("Current JDK doesn't support decoding of charset '"
                        + charset + "' (MIME charset '" + mimeCharset
                        + "' in encoded word '" + body.substring(begin, end)
                        + "')");
            }
            return null;
        }

        if (encodedText.length() == 0) {
            if (log.isWarnEnabled()) {
                log.warn("Missing encoded text in encoded word: '"
                        + body.substring(begin, end) + "'");
            }
            return null;
        }

        try {
            if (encoding.equalsIgnoreCase("Q")) {
                return DecoderUtil.decodeQ(encodedText, charset);
            } else if (encoding.equalsIgnoreCase("B")) {
                return DecoderUtil.decodeB(encodedText, charset);
            } else {
                if (log.isWarnEnabled()) {
                    log.warn("Warning: Unknown encoding in encoded word '"
                            + body.substring(begin, end) + "'");
                }
                return null;
            }
        } catch (UnsupportedEncodingException e) {
            // should not happen because of isDecodingSupported check above
            if (log.isWarnEnabled()) {
                log.warn("Unsupported encoding in encoded word '"
                        + body.substring(begin, end) + "'", e);
            }
            return null;
        } catch (RuntimeException e) {
            if (log.isWarnEnabled()) {
                log.warn("Could not decode encoded word '"
                        + body.substring(begin, end) + "'", e);
            }
            return null;
        }
    }
}
