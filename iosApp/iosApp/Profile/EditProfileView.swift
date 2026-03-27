import SwiftUI
import shared
import PhotosUI

struct EditProfileView: View {
    @ObservedObject var viewModel: ProfileViewModel
    @Environment(\.presentationMode) var presentationMode

    @State private var displayName: String = ""
    @State private var bio: String = ""
    @State private var selectedItem: PhotosPickerItem? = nil
    @State private var profileImage: Image? = nil

    var body: some View {
        Form {
            Section {
                HStack {
                    Spacer()
                    VStack {
                        if let profileImage = profileImage {
                            profileImage
                                .resizable()
                                .scaledToFill()
                                .frame(width: 100, height: 100)
                                .clipShape(Circle())
                        } else {
                            Image(systemName: "person.circle.fill")
                                .resizable()
                                .frame(width: 100, height: 100)
                                .foregroundColor(.gray)
                        }

                        PhotosPicker(selection: $selectedItem, matching: .images, photoLibrary: .shared()) {
                            Text("Change Profile Photo")
                                .foregroundColor(.blue)
                        }
                    }
                    Spacer()
                }
            }

            Section(header: Text("Public Information")) {
                TextField("Display Name", text: $displayName)

                // Bio with character limit
                VStack(alignment: .leading) {
                    TextEditor(text: $bio)
                        .frame(height: 100)
                        .onChange(of: bio) { newValue in
                            if newValue.count > 150 {
                                bio = String(newValue.prefix(150))
                            }
                        }
                    Text("\(bio.count)/150")
                        .font(.caption)
                        .foregroundColor(bio.count == 150 ? .red : .gray)
                        .frame(maxWidth: .infinity, alignment: .trailing)
                }
            }

            Button("Save Changes") {
                Task {
                    await viewModel.updateProfile(displayName: displayName, bio: bio)
                    presentationMode.wrappedValue.dismiss()
                }
            }
        }
        .navigationTitle("Edit Profile")
        .onAppear {
            displayName = viewModel.user?.displayName ?? ""
            bio = viewModel.user?.bio ?? ""
        }
        .onChange(of: selectedItem) { newItem in
            Task {
                if let data = try? await newItem?.loadTransferable(type: Data.self),
                   let uiImage = UIImage(data: data) {
                    profileImage = Image(uiImage: uiImage)
                    // TODO: Implement image upload to KMP StorageService
                }
            }
        }
    }
}
