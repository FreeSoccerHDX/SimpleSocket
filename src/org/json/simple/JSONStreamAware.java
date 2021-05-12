package org.json.simple;

import java.io.IOException;
import java.io.Writer;

public interface JSONStreamAware {

	public abstract void writeJSONString(Writer writer) throws IOException;
}