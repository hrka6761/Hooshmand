package ir.hrka.download.manager

/**
 * Compatibility aliases for public models that now live in [ir.hrka.download.manager.model].
 *
 * Prefer importing from `ir.hrka.download.manager.model` in new code. These aliases keep
 * existing call sites compiling without change.
 */
typealias SingleItemDownloadData = ir.hrka.download.manager.model.SingleItemDownloadData

/** @see ir.hrka.download.manager.model.MultipartItemDownloadData */
typealias MultipartItemDownloadData = ir.hrka.download.manager.model.MultipartItemDownloadData

/** @see ir.hrka.download.manager.model.DownloadType */
typealias DownloadType = ir.hrka.download.manager.model.DownloadType

/** @see ir.hrka.download.manager.model.DownloadStatus */
typealias DownloadStatus = ir.hrka.download.manager.model.DownloadStatus

/** @see ir.hrka.download.manager.model.DownloadStorageLocation */
typealias DownloadStorageLocation = ir.hrka.download.manager.model.DownloadStorageLocation

/** @see ir.hrka.download.manager.model.FileCreationMode */
typealias FileCreationMode = ir.hrka.download.manager.model.FileCreationMode
