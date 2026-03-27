import XCTest
@testable import iosApp
import shared

@MainActor
class AuthViewModelTests: XCTestCase {
    var viewModel: AuthViewModel!

    override func setUp() {
        super.setUp()
        viewModel = AuthViewModel()
    }

    override func tearDown() {
        viewModel = nil
        super.tearDown()
    }

    func testInitialState() {
        XCTAssertFalse(viewModel.isAuthenticated)
        XCTAssertFalse(viewModel.isLoading)
        XCTAssertFalse(viewModel.isEmailSent)
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertEqual(viewModel.email, "")
        XCTAssertEqual(viewModel.password, "")
    }

    func testLoginValidationEmptyFields() {
        viewModel.email = ""
        viewModel.password = ""
        viewModel.login()
        XCTAssertEqual(viewModel.errorMessage, "Email and password cannot be empty")
        XCTAssertFalse(viewModel.isLoading)
    }

    func testSignupValidationEmptyFields() {
        viewModel.email = ""
        viewModel.password = ""
        viewModel.username = ""
        viewModel.signup()
        XCTAssertEqual(viewModel.errorMessage, "All fields are required")
        XCTAssertFalse(viewModel.isLoading)
    }

    func testSignupValidationPasswordMismatch() {
        viewModel.email = "test@example.com"
        viewModel.username = "testuser"
        viewModel.password = "password123"
        viewModel.confirmPassword = "password456"
        viewModel.signup()
        XCTAssertEqual(viewModel.errorMessage, "Passwords do not match")
        XCTAssertFalse(viewModel.isLoading)
    }

    func testSendPasswordResetValidationEmptyField() {
        viewModel.email = ""
        viewModel.sendPasswordReset()
        XCTAssertEqual(viewModel.errorMessage, "Please enter your email")
        XCTAssertFalse(viewModel.isLoading)
    }

    func testLogout() {
        viewModel.isAuthenticated = true
        viewModel.logout()
        XCTAssertFalse(viewModel.isAuthenticated)
    }
}
