package org.iwoss.caelum.util;

import org.iwoss.caelum.CaelumConfig;
import org.slf4j.Logger;

public class CaelumLogger {
    private final Logger logger;

    public CaelumLogger(Logger logger) {
        this.logger = logger;
    }

    public void info(String message) {
        if (CaelumConfig.SERVER.enableLogging.get()) {
            logger.info(message);
        }
    }

    public void warn(String message) {
        if (CaelumConfig.SERVER.enableLogging.get()) {
            logger.warn(message);
        }
    }

    public void debug(String message) {
        if (CaelumConfig.SERVER.enableLogging.get() && logger.isDebugEnabled()) {
            logger.debug(message);
        }
    }

    public void error(String message) {
        if (CaelumConfig.SERVER.enableLogging.get()) {
            logger.error(message);
        }
    }

    public void info(String format, Object... args) {
        if (CaelumConfig.SERVER.enableLogging.get()) {
            logger.info(format, args);
        }
    }

    public void warn(String format, Object... args) {
        if (CaelumConfig.SERVER.enableLogging.get()) {
            logger.warn(format, args);
        }
    }

    public void debug(String format, Object... args) {
        if (CaelumConfig.SERVER.enableLogging.get() && logger.isDebugEnabled()) {
            logger.debug(format, args);
        }
    }

    public void error(String format, Object... args) {
        if (CaelumConfig.SERVER.enableLogging.get()) {
            logger.error(format, args);
        }
    }
}