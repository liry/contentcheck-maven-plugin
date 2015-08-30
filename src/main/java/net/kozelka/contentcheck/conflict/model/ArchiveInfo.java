package net.kozelka.contentcheck.conflict.model;

/**
 * Represents one classpath element, typically an archive
 * @todo get rid of #archiveConflicts here; it should be computed
 * @author Petr Kozelka
 */
public class ArchiveInfo {
    private String key;
    private int classCount;

    public ArchiveInfo() {
    }

    /**
     * @return name of the archive, in presentation-specific form
     */
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setClassCount(int classCount) {
        this.classCount = classCount;
    }

    /**
     * @return number of <code>.class</code> resources
     */
    public int getClassCount() {
        return classCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ArchiveInfo that = (ArchiveInfo) o;
        if (key != null ? !key.equals(that.key) : that.key != null) return false;
//
        return true;
    }

    @Override
    public int hashCode() {
        return key != null ? key.hashCode() : 0;
    }
}
