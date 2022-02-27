package android.content.res;

import java.io.IOException;
import java.io.InputStream;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(AssetManager.class)
public final class AssetManagerHidden {
	public AssetManagerHidden() {
		throw new UnsupportedOperationException("Stub!");
	}

	public final int addAssetPath(String path) {
		throw new UnsupportedOperationException("Stub!");
	}

	public void close() {
		throw new UnsupportedOperationException("Stub!");
	}

	public final InputStream open(String fileName) throws IOException {
		throw new UnsupportedOperationException("Stub!");
	}
}
