package org.clarent.ivyidea.exception;

/**
 * @author Guy Mahieu
 */

public class IvySettingsFileReadException extends IvyIdeaException {

    private String fileName;
    private String moduleName;

    public IvySettingsFileReadException(String fileName, String moduleName, Throwable cause) {
        super(cause);
        this.fileName = fileName;
        this.moduleName = moduleName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getModuleName() {
        return moduleName;
    }

    @Override
    public String getMessage() {
        return "An error occured while reading the ivy settings for module " + moduleName + " from " + fileName;
    }
}
