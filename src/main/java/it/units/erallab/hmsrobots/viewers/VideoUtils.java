package it.units.erallab.hmsrobots.viewers;

import io.humble.video.*;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author eric
 * @created 2020/11/07
 * @project 2dhmsr
 */
public class VideoUtils {

  public enum EncoderFramework {JCODEC, HUMBLE}

  private static final EncoderFramework DEFAULT_ENCODER = EncoderFramework.HUMBLE;
  private static final Logger L = Logger.getLogger(VideoUtils.class.getName());

  private VideoUtils() {
  }

  public static void encodeAndSave(List<BufferedImage> images, double frameRate, File file) throws IOException {
    encodeAndSave(images, frameRate, file, DEFAULT_ENCODER);
  }

  public static void encodeAndSave(List<BufferedImage> images, double frameRate, File file, EncoderFramework encoder) throws IOException {
    switch (encoder) {
      case JCODEC -> encodeAndSaveWithJCodec(images, frameRate, file);
      case HUMBLE -> encodeAndSaveWithHumble(images, frameRate, file);
    }
  }

  private static void encodeAndSaveWithJCodec(List<BufferedImage> images, double frameRate, File file) throws IOException {
    SeekableByteChannel channel = NIOUtils.writableChannel(file);
    AWTSequenceEncoder encoder = new AWTSequenceEncoder(channel, Rational.R((int) Math.round(frameRate), 1));
    //encode
    try {
      for (BufferedImage image : images) {
        encoder.encodeImage(image);
      }
    } catch (IOException ex) {
      L.severe(String.format("Cannot encode image due to %s", ex));
    }
    encoder.finish();
    NIOUtils.closeQuietly(channel);
  }

  private static void encodeAndSaveWithHumble(List<BufferedImage> images, double frameRate, File file) throws IOException {
    //inspiration from https://github.com/artclarke/humble-video/blob/master/humble-video-demos/src/main/java/io/humble/video/demos/RecordAndEncodeVideo.java
    io.humble.video.Rational rational = io.humble.video.Rational.make(1, (int) Math.round(frameRate));
    final Muxer muxer = Muxer.make(file.getPath(), null, null);
    final MuxerFormat format = muxer.getFormat();
    L.info(String.format("Muxing with %s format", format.getLongName()));
    final Codec codec = Codec.findEncodingCodec(format.getDefaultVideoCodecId());
    Encoder encoder = Encoder.make(codec);
    encoder.setWidth(images.get(0).getWidth());
    encoder.setHeight(images.get(0).getHeight());
    final PixelFormat.Type pixelFormat = PixelFormat.Type.PIX_FMT_YUV420P;
    encoder.setPixelFormat(pixelFormat);
    encoder.setTimeBase(rational);
    if (format.getFlag(MuxerFormat.Flag.GLOBAL_HEADER)) {
      encoder.setFlag(Encoder.Flag.FLAG_GLOBAL_HEADER, true);
    }
    encoder.open(null, null);
    muxer.addNewStream(encoder);
    try {
      muxer.open(null, null);
    } catch (InterruptedException e) {
      throw new IOException(String.format("Cannot open muxer due to %s", e));
    }
    final MediaPicture picture = MediaPicture.make(
        encoder.getWidth(),
        encoder.getHeight(),
        pixelFormat
    );
    picture.setTimeBase(rational);
    MediaPictureConverter converter = MediaPictureConverterFactory.createConverter(images.get(0), picture);
    final MediaPacket packet = MediaPacket.make();
    for (int i = 0; i < images.size(); i++) {
      BufferedImage image = images.get(i);
      converter.toPicture(picture, image, i);
      do {
        encoder.encode(packet, picture);
        if (packet.isComplete()) {
          muxer.write(packet, false);
        }
      } while (packet.isComplete());
    }
    do {
      encoder.encode(packet, null);
      if (packet.isComplete()) {
        muxer.write(packet, false);
      }
    } while (packet.isComplete());
    muxer.close();
  }

}
