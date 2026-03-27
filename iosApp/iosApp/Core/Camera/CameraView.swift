import SwiftUI
import AVFoundation

struct CameraView: UIViewControllerRepresentable {
    @Binding var isRecording: Bool
    @Binding var capturedMediaURL: URL?
    @Binding var isTakingPhoto: Bool
    @Binding var isFrontCamera: Bool
    @Binding var isFlashOn: Bool

    func makeUIViewController(context: Context) -> CameraViewController {
        let controller = CameraViewController()
        controller.delegate = context.coordinator
        return controller
    }

    func updateUIViewController(_ uiViewController: CameraViewController, context: Context) {
        if isRecording {
            uiViewController.startRecording()
        } else {
            uiViewController.stopRecording()
        }

        if isTakingPhoto {
            uiViewController.capturePhoto()
            DispatchQueue.main.async {
                isTakingPhoto = false
            }
        }

        uiViewController.switchCamera(isFront: isFrontCamera)
        uiViewController.setFlash(isOn: isFlashOn)
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, CameraViewControllerDelegate {
        var parent: CameraView

        init(_ parent: CameraView) {
            self.parent = parent
        }

        func didCaptureMedia(at url: URL) {
            DispatchQueue.main.async {
                self.parent.capturedMediaURL = url
            }
        }
    }
}

protocol CameraViewControllerDelegate: AnyObject {
    func didCaptureMedia(at url: URL)
}

class CameraViewController: UIViewController, AVCaptureFileOutputRecordingDelegate, AVCapturePhotoCaptureDelegate {
    weak var delegate: CameraViewControllerDelegate?
    var captureSession: AVCaptureSession!
    var videoOutput: AVCaptureMovieFileOutput!
    var photoOutput: AVCapturePhotoOutput!
    var previewLayer: AVCaptureVideoPreviewLayer!

    private var isFrontCamera: Bool = false
    private var isFlashOn: Bool = false

    override func viewDidLoad() {
        super.viewDidLoad()
        setupCamera()
    }

    func setupCamera() {
        captureSession = AVCaptureSession()
        captureSession.sessionPreset = .high

        guard let videoCaptureDevice = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: isFrontCamera ? .front : .back),
              let audioCaptureDevice = AVCaptureDevice.default(for: .audio) else { return }

        do {
            let videoInput = try AVCaptureDeviceInput(device: videoCaptureDevice)
            let audioInput = try AVCaptureDeviceInput(device: audioCaptureDevice)

            if captureSession.canAddInput(videoInput) { captureSession.addInput(videoInput) }
            if captureSession.canAddInput(audioInput) { captureSession.addInput(audioInput) }

            videoOutput = AVCaptureMovieFileOutput()
            if captureSession.canAddOutput(videoOutput) { captureSession.addOutput(videoOutput) }

            photoOutput = AVCapturePhotoOutput()
            if captureSession.canAddOutput(photoOutput) { captureSession.addOutput(photoOutput) }

            previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
            previewLayer.frame = view.layer.bounds
            previewLayer.videoGravity = .resizeAspectFill
            view.layer.addSublayer(previewLayer)

            DispatchQueue.global(qos: .userInitiated).async { [weak self] in
                self?.captureSession.startRunning()
            }
        } catch {
            print("Error setting up camera: \(error)")
        }
    }

    func startRecording() {
        guard captureSession.isRunning, !videoOutput.isRecording else { return }
        let tempURL = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString).appendingPathExtension("mov")
        videoOutput.startRecording(to: tempURL, recordingDelegate: self)
    }

    func stopRecording() {
        if videoOutput.isRecording {
            videoOutput.stopRecording()
        }
    }

    func capturePhoto() {
        let settings = AVCapturePhotoSettings()

        if photoOutput.supportedFlashModes.contains(.on) {
            settings.flashMode = isFlashOn ? .on : .off
        } else {
            settings.flashMode = .off
        }

        photoOutput.capturePhoto(with: settings, delegate: self)
    }

    func switchCamera(isFront: Bool) {
        guard isFront != self.isFrontCamera else { return }
        self.isFrontCamera = isFront

        captureSession.beginConfiguration()

        // Remove existing video input
        if let inputs = captureSession.inputs as? [AVCaptureDeviceInput] {
            for input in inputs {
                if input.device.hasMediaType(.video) {
                    captureSession.removeInput(input)
                }
            }
        }

        // Add new video input
        guard let newDevice = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: isFront ? .front : .back) else {
            captureSession.commitConfiguration()
            return
        }

        do {
            let newInput = try AVCaptureDeviceInput(device: newDevice)
            if captureSession.canAddInput(newInput) {
                captureSession.addInput(newInput)
            }
        } catch {
            print("Error switching camera: \(error)")
        }

        captureSession.commitConfiguration()
    }

    func setFlash(isOn: Bool) {
        self.isFlashOn = isOn
        // Note: For video recording torch mode would need to be handled separately
    }

    // MARK: - AVCaptureFileOutputRecordingDelegate

    func fileOutput(_ output: AVCaptureFileOutput, didFinishRecordingTo outputFileURL: URL, from connections: [AVCaptureConnection], error: Error?) {
        if let error = error {
            print("Recording failed: \(error)")
        } else {
            delegate?.didCaptureMedia(at: outputFileURL)
        }
    }

    // MARK: - AVCapturePhotoCaptureDelegate

    func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        if let error = error {
            print("Photo capture failed: \(error)")
            return
        }

        guard let data = photo.fileDataRepresentation() else { return }
        let tempURL = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString).appendingPathExtension("jpg")

        do {
            try data.write(to: tempURL)
            delegate?.didCaptureMedia(at: tempURL)
        } catch {
            print("Failed to save captured photo: \(error)")
        }
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        previewLayer?.frame = view.bounds
    }
}
