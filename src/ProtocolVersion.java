/**
 * Represents a httpVersion version. The "major.minor" numbering scheme is used to indicate versions of the httpVersion.
 */
public class ProtocolVersion {
    protected String protocol;
    protected int major;
    protected int minor;

    ProtocolVersion(String i_protocol, int i_major, int i_minor) {
        this.protocol = i_protocol;
        this.major = i_major;
        this.minor = i_minor;
    }

    public ProtocolVersion(String protocolVersion) throws AssertionError {
        String[] tokens = protocolVersion.split("/");
        assert tokens.length == 2 : "Failed to parse the protocol part.";
        protocol = tokens[0];
        String version = tokens[1];
        String[] versionDetails = version.split("\\.");
        assert versionDetails.length == 2 : "Failed to parse major or minor version.";
        try {
            major = Integer.parseInt(versionDetails[0]);
            minor = Integer.parseInt(versionDetails[1]);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Failed to parse major or minor version: Not an integer.");
        }

    }

    @Override
    public final String toString() {
        return String.format("%s/%s.%s", protocol, major, minor);
    }

    @Override
    public boolean equals(Object other) {
        boolean a;
        boolean b;
        try {
            a = this.isComparable((ProtocolVersion) other);
            b = this.compareToVersion((ProtocolVersion)other) == 0;
        } catch (ClassCastException e) {
            return false;
        }
        return  (a && b);
    }

    public boolean isComparable(ProtocolVersion other) {
        return this.protocol.equals(other.protocol);
    }

    public int compareToVersion(ProtocolVersion other) {
        if (this.lessThan(other)) {
            return -1;
        } else if (this.greaterThan(other)) {
            return 1;
        } else {
            return 0;
        }
    }

    public boolean greaterThan(ProtocolVersion other) {
        if (this.major > other.major) {
            return true;
        } else if (this.major == other.major && this.minor > other.minor) {
            return true;
        } else {
            return false;
        }
    }

    public boolean lessThan(ProtocolVersion other) {
        if (this.major < other.major) {
            return true;
        } else if (this.major == other.major && this.minor < other.minor) {
            return true;
        } else {
            return false;
        }
    }
}
