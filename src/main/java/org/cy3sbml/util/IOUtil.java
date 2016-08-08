package org.cy3sbml.util;


import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Helper functions for input and output.
 */
public class IOUtil {
    private static final int BUFFER_SIZE = 16384;

    /** Read resource to InputStream */
    public static InputStream readResource(String resource){
        return IOUtil.class.getResourceAsStream(resource);
    }

    /** Read String from InputStream. */
    public static String inputStream2String(InputStream source) throws IOException {
        StringWriter writer = new StringWriter();
        BufferedReader reader = new BufferedReader(new InputStreamReader(source));
        try {
            char[] buffer = new char[BUFFER_SIZE];
            int charactersRead = reader.read(buffer, 0, buffer.length);
            while (charactersRead != -1) {
                writer.write(buffer, 0, charactersRead);
                charactersRead = reader.read(buffer, 0, buffer.length);
            }
        } finally {
            reader.close();
        }
        return writer.toString();
    }

    /** Create InputStream from String. */
    public static InputStream string2InputStream(String s){
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }


    /** Copy InputStream. */
    public static InputStream copyInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream copy = new ByteArrayOutputStream();
        int chunk = 0;
        byte[] data = new byte[1024*1024];
        while((-1 != (chunk = is.read(data)))) {
            copy.write(data, 0, chunk);
        }
        is.close();
        return new ByteArrayInputStream( copy.toByteArray() );
    }


    /**
     * Creates a unique file with a given filename and a given extension in a given directory.
     * If the file already exists, suffixes will be added.
     *
     * @param FileName - Filename of the Temporary file
     * @param Extension - File extension of the temporary file (with dot).
     * @return The unique File Object.
     */
    public static File createUniqueFile(File directory, String FileName, String Extension) {
        File target = new File(directory, FileName + Extension);
        int suffix = 0;
        while(target.exists()) {
            target = new File(directory, FileName + "_" + suffix + Extension);
            suffix++;
        }
        return target;
    }

}
