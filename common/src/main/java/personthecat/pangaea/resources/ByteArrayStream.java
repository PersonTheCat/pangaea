package personthecat.pangaea.resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ByteArrayStream extends ByteArrayOutputStream {
    public InputStream asInputStream() {
        return new ByteArrayInputStream(this.buf);
    }
}
