package config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import net.server.Server;
import org.slf4j.LoggerFactory;

import java.io.File;

public class LogBackConfig {
    public static void configure() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = lc.getLogger(Logger.ROOT_LOGGER_NAME);

        // Define the path to the log file within your app's data directory
        File logFile = new File(Server.getInstance().getContext().getFilesDir(), "server.log");
        if (logFile.exists()) {
            logFile.delete();
        }

        FileAppender<ILoggingEvent> fileAppender = setFileLogger(lc, logFile);
        LogcatAppender logcatAppender = setLogcatLogger(lc);

        rootLogger.addAppender(fileAppender);
        rootLogger.addAppender(logcatAppender);
        rootLogger.setLevel(Level.INFO);
    }

    private static FileAppender<ILoggingEvent> setFileLogger(LoggerContext lc, File logFile) {
        // Create a FileAppender
        FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        fileAppender.setFile(logFile.getAbsolutePath()); // Set the file path
        fileAppender.setContext(lc);

        // Set the encoder
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder.setContext(lc);
        encoder.start();

        fileAppender.setEncoder(encoder);
        fileAppender.start();

        return fileAppender;
    }

    private static LogcatAppender setLogcatLogger(LoggerContext lc) {
        LogcatAppender logcatAppender = new LogcatAppender();
        logcatAppender.setContext(lc);

        PatternLayoutEncoder logcatEncoder = new PatternLayoutEncoder();
        logcatEncoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        logcatEncoder.setContext(lc);
        logcatEncoder.start();

        logcatAppender.setEncoder(logcatEncoder);
        logcatAppender.start();

        return logcatAppender;
    }
}
