package fr.ippon.tatami.service;

import fr.ippon.tatami.config.Constants;
import fr.ippon.tatami.domain.Attachment;
import fr.ippon.tatami.domain.Avatar;
import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.repository.AvatarRepository;
import fr.ippon.tatami.repository.DomainConfigurationRepository;
import fr.ippon.tatami.repository.UserRepository;
import fr.ippon.tatami.security.AuthenticationService;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Date;

@Service
public class AvatarService {

    private static final Logger log = LoggerFactory.getLogger(AvatarService.class);

    @Inject
    private AvatarRepository avatarRepository;

    @Inject
    private DomainConfigurationRepository domainConfigurationRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private AuthenticationService authenticationService;

    public String createAvatar(Avatar avatar) {

        User currentUser = authenticationService.getCurrentUser();

        if (currentUser.getAvatar() != null && !("").equals(currentUser.getAvatar())) {
            deleteAvatar(currentUser.getAvatar());
        }

        try {
            avatar.setContent(scaleImage(avatar.getContent()));
        } catch (IOException e) {
            log.info("Avatar could not be resized : " + e.getMessage());
            return null;
        }

        avatarRepository.createAvatar(avatar);

        log.debug("Avatar created : {}", avatar);

        return avatar.getAvatarId();
    }

    public Avatar getAvatarById(String avatartId) {
        log.debug("Get Avatar Id : {}", avatartId);
        return avatarRepository.findAvatarById(avatartId);
    }

    public void deleteAvatar(String avatarId) {
        avatarRepository.removeAvatar(avatarId);

        User currentUser = authenticationService.getCurrentUser();
        userRepository.updateUser(currentUser);
    }

    private byte[] scaleImage(byte[] data) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);

        BufferedImage img = ImageIO.read(in);
        int width = Constants.AVATAR_SIZE;
        int height = Constants.AVATAR_SIZE;

        Image image = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        bufferedImage.getGraphics().drawImage(image, 0, 0, new Color(0, 0, 0), null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "jpg", byteArrayOutputStream);

        log.debug("New Byte size of Avatar : {} Kbits", byteArrayOutputStream.size() / 1024);

        return byteArrayOutputStream.toByteArray();
    }

    public Avatar createAvatarBasedOnAvatar(Avatar avatar) {
        User currentUser = authenticationService.getCurrentUser();
        Avatar dbAvatar = avatarRepository.findAvatarByFilename(avatar.getFilename());
        if (dbAvatar != null) {
            return dbAvatar;
        } else if (avatar.getFilename().startsWith("http")) {
            byte[] bytes = {};
            try {
                bytes = fetchRemoteFile(avatar.getFilename());
            } catch (Exception e) {
                log.warn("Could not load bytes from: " + avatar.getFilename());
            }
            if (bytes == null) {
                log.warn("Could not get any bytes and is null from url: " + avatar.getFilename());
                return avatar;
            }
            avatar.setSize(bytes.length);
            avatar.setContent(bytes);
            avatar.setCreationDate(new Date());
            resizeAvatarAndSetThumbnail(avatar);
            avatarRepository.createAvatar(avatar);
        }
        return avatar;
    }

    private void resizeAvatarAndSetThumbnail(Avatar avatar) {
        byte[] result = new byte[0];
        boolean isImage = false;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(avatar.getContent()))
                    .width(Constants.AVATAR_SIZE)
                    .height(Constants.AVATAR_SIZE)
                    .toOutputStream(baos);
            baos.flush();
            result = baos.toByteArray();
        } catch (IOException e) {
            log.error("Error resizing avatar " + avatar.getAvatarId());
        }
        if (result.length > 0) {
            avatar.setSize(result.length);
            avatar.setContent(result);
        }
        generateAvatarThumbnail(avatar);
    }

    private void generateAvatarThumbnail(Avatar avatar) {
        byte[] result = new byte[0];
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(avatar.getContent()))
                    .size(Constants.AVATAR_THUMBNAIL_SIZE,Constants.AVATAR_THUMBNAIL_SIZE)
                    .toOutputStream(baos);
            baos.flush();
            result = baos.toByteArray();
        } catch(IOException e) {
            log.error("Error creating thumbnail for avatar "+avatar.getAvatarId());
        }
        avatar.setThumb(result);
    }

    private byte[] fetchRemoteFile(String location) throws Exception {
        URL url = new URL(location);
        InputStream is = null;
        byte[] bytes = null;
        try {
            is = url.openStream ();
            bytes = IOUtils.toByteArray(is);
        } catch (IOException e) {
            //handle errors
        }
        finally {
            if (is != null) is.close();
        }
        return bytes;
    }
}
