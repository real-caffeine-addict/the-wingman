INSERT INTO downloaded_media (
    file_name,
    mime_type,
    byte_size,
    received_time,
    path,
    message_id,
    sender_id,
    decrypted_hash
)values (
        :fileName,
        :mimeType,
        :byteSize,
        :received,
        :path,
        :messageId,
        :senderJid,
        :hash
        )