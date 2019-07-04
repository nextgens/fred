package freenet.client.filter;

import org.junit.Ignore;
import org.junit.Test;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import static org.junit.Assert.*;

public class TheoraBitstreamFilterTest {

    @Test
    public void parseIdentificationHeaderTest() throws IOException {
        try (DataInputStream input = new DataInputStream(getClass().getResourceAsStream("./ogg/theora_header.ogg"))) {
            OggPage page = OggPage.readPage(input);

            TheoraBitstreamFilter theoraBitstreamFilter = new TheoraBitstreamFilter(page);
            assertEquals(page.asPackets(), theoraBitstreamFilter.parse(page).asPackets());
        }
    }

    @Test @Ignore // TODO: add valid Theora video
    public void parseTest() throws IOException {
        try (DataInputStream input = new DataInputStream(getClass().getResourceAsStream("./ogg/..."))) {
            OggPage page = OggPage.readPage(input);
            int pageSerial = page.getSerial();
            TheoraBitstreamFilter theoraBitstreamFilter = new TheoraBitstreamFilter(page);

            while (true) {
                try {
                    if (page.getSerial() == pageSerial) {
                        theoraBitstreamFilter.parse(page);
                    }

                    page = OggPage.readPage(input);
                } catch (EOFException e) {
                    break;
                }
            }
        }
    }

    @Test(expected = UnknownContentTypeException.class)
    public void parseInvalidHeaderTest() throws IOException {
        try (DataInputStream input = new DataInputStream(getClass().getResourceAsStream("./ogg/invalid_header.ogg"))) {
            OggPage page = OggPage.readPage(input);

            TheoraBitstreamFilter theoraBitstreamFilter = new TheoraBitstreamFilter(page);
            theoraBitstreamFilter.parse(page);
        }
    }
}
