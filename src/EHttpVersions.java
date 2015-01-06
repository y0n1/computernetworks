/**
 * Created by Yoni on 1/1/2015.
 */
public enum EHttpVersions {
    HTTP_1_1 (new ProtocolVersion("HTTP", 1, 1)),
    HTTP_1_0 (new ProtocolVersion("HTTP", 1, 0));

    private final ProtocolVersion protocolVersion;

    EHttpVersions(ProtocolVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }


    @Override
    public String toString() {
        return this.protocolVersion.toString();
    }

    public ProtocolVersion value() {
        return this.protocolVersion;
    }
}
