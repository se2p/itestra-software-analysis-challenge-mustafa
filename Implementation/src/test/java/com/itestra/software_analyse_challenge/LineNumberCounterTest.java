package com.itestra.software_analyse_challenge;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LineNumberCounterTest {
    private File createTempFileWithContent(String content) throws IOException {
        File tempFile = File.createTempFile("test", ".java");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
        }
        return tempFile;
    }

    @Test
    void testComputeLineNumberBonus_EmptyFile_ReturnsZero() throws IOException {
        File file = createTempFileWithContent("");
        assertEquals(0, LineNumberCounter.computeLineNumberBonus(file));
    }

    @Test
    void testComputeLineNumberBonus_FileWithOnlyComments_ReturnsZero() throws IOException {
        File file = createTempFileWithContent("// comment line 1\n\t// comment line 2\n");
        assertEquals(0, LineNumberCounter.computeLineNumberBonus(file));
    }

    @Test
    void testComputeLineNumberBonus_FileWithBlankLines_ReturnsZero() throws IOException {
        File file = createTempFileWithContent("\n   \n\t\n");
        assertEquals(0, LineNumberCounter.computeLineNumberBonus(file));
    }

    @Test
    void testComputeLineNumberBonus_FileWithMixedCommentsAndCode() throws IOException {
        File file = createTempFileWithContent("""
                // this is a comment
                int a = 10;
                
                // another comment
                int b = 20;
                """);
        assertEquals(2, LineNumberCounter.computeLineNumberBonus(file));
    }

    @Test
    void testComputeLineNumberBonus_FileWithOnlyCodeLines() throws IOException {
        File file = createTempFileWithContent("""
                int a = 10;
                int b = 20;
                System.out.println(a + b);
                """);
        assertEquals(3, LineNumberCounter.computeLineNumberBonus(file));
    }

    @Test
    void testComputeLineNumberBonus_FileWithLeadingWhitespaceCodeLines() throws IOException {
        File file = createTempFileWithContent("""
                   int a = 10;
                public HtmlElement html() { return elem("html"); }
                \tint b = 20;
                """);
        assertEquals(3, LineNumberCounter.computeLineNumberBonus(file));
    }

    @Test
    void testComputeLineNumberBonus_FileWithMoreComplexCode() throws IOException {
        File file = createTempFileWithContent("""
                package spark.http.matching;
                
                import java.io.IOException;
                import java.io.OutputStream;
                
                import javax.servlet.http.HttpServletRequest;
                import javax.servlet.http.HttpServletResponse;
                
                import spark.utils.GzipUtils;
                import spark.serialization.SerializerChain;
                
                /**
                 * Represents the 'body'
                 */
                final class Body {
                
                    private Object content;
                
                    public static Body create() {
                        return new Body();
                    }
                
                    private Body() {
                
                    }
                
                    public boolean notSet() {
                        return content == null;
                    }
                
                    public boolean isSet() {
                        return content != null;
                    }
                
                    public Object get() {
                        return content;
                    }
                
                    public void set(Object content) {
                        this.content = content;
                    }
                
                    public void serializeTo(HttpServletResponse httpResponse,
                                            SerializerChain serializerChain,
                                            HttpServletRequest httpRequest) throws IOException {
                
                        if (!httpResponse.isCommitted()) {
                            if (httpResponse.getContentType() == null) {
                                httpResponse.setContentType("text/html; charset=utf-8");
                            }
                
                            // Check if GZIP is wanted/accepted and in that case handle that
                            OutputStream responseStream = GzipUtils.checkAndWrap(httpRequest, httpResponse, true);
                
                            // Serialize the body to output stream
                            serializerChain.process(responseStream, content);
                
                            responseStream.flush(); // needed for GZIP stream. Not sure where the HTTP response actually gets cleaned up
                            responseStream.close(); // needed for GZIP
                        }
                    }
                
                
                }
                """);
        assertEquals(40, LineNumberCounter.computeLineNumberBonus(file));
    }

    @Test
    void testComputeLineNumberBonus_IOExceptionIsHandledGracefully() {
        File nonExistentFile = new File("nonexistent.java");
        int result = LineNumberCounter.computeLineNumberBonus(nonExistentFile);
        assertEquals(0, result);
    }

    @Test
    void testComputeLineNumberBonus_FileWithOneLineBlockComment() throws IOException {
        File file = createTempFileWithContent("""
                /* Copyright 2016 - Per Wendel */
                """);
        assertEquals(0, LineNumberCounter.computeLineNumberBonus(file));
    }

    @Test
    void testComputeLineNumberBonus_FileWithBlockCommentWithoutAsterisk() throws IOException {
        File file = createTempFileWithContent("""
                /*
                   Block comment without extra asterisk
                */
                """);
        assertEquals(0, LineNumberCounter.computeLineNumberBonus(file));
    }

    @Test
    void testComputeLineNumberBonus_FileWithMultiLineBlockCommentsWithAsterisk() throws IOException {
        File file = createTempFileWithContent("""
                /*
                 * Copyright 2016 - Per Wendel
                 *
                 *  Licensed under the Apache License, Version 2.0 (the "License");
                 *  you may not use this file except in compliance with the License.
                 *  You may obtain a copy of the License at
                 *
                 *
                 *      http://www.apache.org/licenses/LICENSE-2.0
                 *
                 * Unless required by applicable law or agreed to in writing, software
                 * distributed under the License is distributed on an "AS IS" BASIS,
                 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
                 * See the License for the specific language governing permissions and
                 * limitations under the License.
                 */
                """);
        assertEquals(0, LineNumberCounter.computeLineNumberBonus(file));
    }

    @Test
    void testComputeLineNumberBonus_FileWithBlockCommentAndCode() throws IOException {
        File file = createTempFileWithContent("""
                /**
                  * Constructor
                  *
                  * @param routeMatcher      The route matcher
                  * @param staticFiles       The static files configuration object
                  * @param externalContainer Tells the filter that Spark is run in an external web container.
                  *                          If true, chain.doFilter will be invoked if request is not consumed by Spark.
                  * @param hasOtherHandlers  If true, do nothing if request is not consumed by Spark in order to let others handlers process the request.
                  */
                 public MatcherFilter(spark.route.Routes routeMatcher,
                                      StaticFilesConfiguration staticFiles,
                                      ExceptionMapper exceptionMapper,
                                      boolean externalContainer,
                                      boolean hasOtherHandlers) {
    
                     this.routeMatcher = routeMatcher;
                     this.staticFiles = staticFiles;
                     this.exceptionMapper = exceptionMapper;
                     this.externalContainer = externalContainer;
                     this.hasOtherHandlers = hasOtherHandlers;
                     this.serializerChain = new SerializerChain();
                 }
    
                 @Override
                 public void init(FilterConfig config) {
                     //
                 }
                """);
        assertEquals(15, LineNumberCounter.computeLineNumberBonus(file));
    }

    @Test
    void testComputeLineNumberBonus_FileWithSimpleGetterMethod() throws IOException {
        File file = createTempFileWithContent("""
                public int getSize() {
                    return size;
                }
                """);
        assertEquals(0, LineNumberCounter.computeLineNumberBonus(file));
    }

    @Test
    void testComputeLineNumberBonus_FileWithTwoGetterMethods() throws IOException {
        File file = createTempFileWithContent("""
                public Instant getEventDateTime() {
                    return this.eventDateTime;
                }
            
                public List<Student> getStudents(){
                    return students;
                }
                """);
        assertEquals(0, LineNumberCounter.computeLineNumberBonus(file));
    }

    @Test
    void testComputeLineNumberBonus_FileWithoutGetterMethod() throws IOException {
        File file = createTempFileWithContent("""
                @PreAuthorize("hasAuthority('MANAGE_ALL_USERS')")
                @GetMapping("/users/{id}")
                public WrapResponseWithContentKey<?> getUserById(@PathVariable Long id) {
                    UserResponseDTO userDTO = userService.getUserById(id);
                    return new WrapResponseWithContentKey<>(userDTO);
                }
                
                public long getCurrTimeLong() {
                    long offset = 0L;
                    if (isRunning()) {
                        offset = System.currentTimeMillis() - startTime;
                    }
                    return ms + offset;
                }
                
                public String getUpMessage = "Getting Up";
                
                public void getUp() {
                    System.out.println(getUpMessage);
                }
                """);
        assertEquals(17, LineNumberCounter.computeLineNumberBonus(file));
    }

    @Test
    void testComputeLineNumberBonus_FileWithBlockCommentAndGetterMethod() throws IOException {
        File file = createTempFileWithContent("""
                /**
                * Returns date and time of the scheduled event
                */
                public Instant getEventDateTime(){
                    return this.eventDateTime;
                }
            
                public TestEventStatus getStatus(){
                    return status;
                }
                """);
        assertEquals(0, LineNumberCounter.computeLineNumberBonus(file));
    }

    @Test
    void testComputeLineNumberBonus_FileWithEverythingMixed() throws IOException {
        File file = createTempFileWithContent("""
                /*
                 * This is a block comment
                 * spanning multiple lines
                 */
                public class Example {
                
                    // Single-line comment at the top
                
                    private int value;
                
                    public int getValue() {
                        return value;
                    }
                
                    public void setValue(int value) {
                        this.value = value; // should be counted
                    }
                
                    public void doSomething() {
                        int a = 10;
                        int b = 20;
                        System.out.println(a + b); // valid line
                    }
                
                    /*
                     * Another block comment
                     * that spans multiple lines
                     */
                
                    public void anotherMethod() {
                        String text = "Hello";
                        System.out.println(text);
                    }
                
                    // Last single-line comment
                }
                
                """);
        assertEquals(15, LineNumberCounter.computeLineNumberBonus(file));
    }
}
