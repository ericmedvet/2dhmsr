package it.units.erallab.hmsrobots.viewers;

import io.humble.video.*;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;
import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.Format;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;
import org.jcodec.scale.AWTUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author eric
 * @created 2020/11/07
 * @project 2dhmsr
 */
public class VideoUtils {

  public enum EncoderFacility {JCODEC, HUMBLE, FFMPEG_LARGE, FFMPEG_SMALL}

  private static final EncoderFacility DEFAULT_ENCODER = EncoderFacility.JCODEC;
  private static final Logger L = Logger.getLogger(VideoUtils.class.getName());

  private VideoUtils() {
  }

  public static void encodeAndSave(List<BufferedImage> images, double frameRate, File file) throws IOException {
    encodeAndSave(images, frameRate, file, DEFAULT_ENCODER);
  }

  public static void encodeAndSave(List<BufferedImage> images, double frameRate, File file, EncoderFacility encoder) throws IOException {
    switch (encoder) {
      case JCODEC -> encodeAndSaveWithJCodec(images, frameRate, file);
      case HUMBLE -> encodeAndSaveWithHumble(images, frameRate, file);
      case FFMPEG_LARGE -> encodeAndSaveWithFFMpeg(images, frameRate, file, 18);
      case FFMPEG_SMALL -> encodeAndSaveWithFFMpeg(images, frameRate, file, 30);
    }
  }

  private static void encodeAndSaveWithJCodec(List<BufferedImage> images, double frameRate, File file) throws IOException {
    SeekableByteChannel channel = NIOUtils.writableChannel(file);
    SequenceEncoder encoder = new SequenceEncoder(
        channel,
        Rational.R((int) Math.round(frameRate), 1),
        Format.MOV,
        org.jcodec.common.Codec.H264,
        null
    );
    //encode
    try {
      for (BufferedImage image : images) {
        encoder.encodeNativeFrame(AWTUtil.fromBufferedImageRGB(image));
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

  private static void encodeAndSaveWithFFMpeg(List<BufferedImage> images, double frameRate, File file, int compression) throws IOException {
    //save all files
    String workingDirName = file.getAbsoluteFile().getParentFile().getPath();
    String imagesDirName = workingDirName + File.separator + "imgs." + System.currentTimeMillis();
    Files.createDirectories(Path.of(imagesDirName));
    List<Path> toDeletePaths = new ArrayList<>();
    L.fine(String.format("Saving %d files in %s", images.size(), imagesDirName));
    for (int i = 0; i < images.size(); i++) {
      File imageFile = new File(imagesDirName + File.separator + String.format("frame%06d", i) + ".jpg");
      ImageIO.write(images.get(i), "jpg", imageFile);
      toDeletePaths.add(imageFile.toPath());
    }
    toDeletePaths.add(Path.of(imagesDirName));
    //invoke ffmpeg
    String command = String.format(
        "ffmpeg -y -r %d -i %s/frame%%06d.jpg -vcodec libx264 -crf %d -pix_fmt yuv420p %s",
        (int) Math.round(frameRate),
        imagesDirName,
        compression,
        file.getPath()
    );
    L.fine(String.format("Running: %s", command));
    ProcessBuilder pb = new ProcessBuilder(command.split(" "));
    pb.directory(new File(workingDirName));
    StringBuilder sb = new StringBuilder();
    try {
      Process process = pb.start();
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line).append("\n");
      }
      reader.close();
      int exitVal = process.waitFor();
      if (exitVal < 0) {
        throw new IOException(String.format("Unexpected exit val: %d. Full output is:%n%s", exitVal, sb.toString()));
      }
    } catch (IOException | InterruptedException e) {
      throw (e instanceof IOException) ? (IOException) e : (new IOException(e));
    } finally {
      //delete all files
      L.fine(String.format("Deleting %d paths", toDeletePaths.size()));
      for (Path path : toDeletePaths) {
        try {
          Files.delete(path);
        } catch (IOException e) {
          L.log(Level.WARNING, String.format("Cannot delete %s", path), e);
        }
      }
    }
  }

}
