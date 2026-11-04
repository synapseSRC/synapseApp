import SwiftUI
import shared

struct PasskeysView: View {
    @StateObject private var viewModel = DependencyContainer.shared.makePasskeysViewModel()

    var body: some View {
        ZStack {
            List {
                if viewModel.passkeys.isEmpty {
                    Section {
                        emptyStateView
                            .listRowBackground(Color.clear)
                            .listRowInsets(EdgeInsets())
                    }
                } else {
                    Section(header: Text(String(localized: "passkeys_section_title"))) {
                        ForEach(viewModel.passkeys, id: \.id) { passkey in
                            PasskeyRow(passkey: passkey)
                        }
                        .onDelete(perform: deletePasskeys)
                    }
                }

                Section {
                    Button(action: {
                        viewModel.add()
                    }) {
                        Label(String(localized: "passkeys_add_button"), systemImage: "plus.circle.fill")
                            .frame(maxWidth: .infinity, alignment: .center)
                    }
                    .disabled(viewModel.isLoading || viewModel.isAdding)
                }
            }
            .listStyle(InsetGroupedListStyle())

            if viewModel.isLoading {
                LoadingView(message: String(localized: "passkeys_loading"))
            }
            if viewModel.isAdding {
                LoadingView(message: String(localized: "passkeys_adding"))
            }
        }
        .navigationTitle(String(localized: "settings_passkeys"))
        .navigationBarTitleDisplayMode(.inline)
        .alert(String(localized: "label_error"), isPresented: showsError) {
            Button(String(localized: "action_ok"), role: .cancel) {}
        } message: {
            Text(viewModel.errorMessage ?? "")
        }
        .onAppear {
            viewModel.load()
        }
    }

    private var emptyStateView: some View {
        VStack(spacing: AppTheme.Spacing.space16) {
            Image(systemName: "person.badge.key")
                .font(.system(size: 48))
                .foregroundColor(.secondary)
            Text(String(localized: "passkeys_empty_title"))
                .font(.headline)
            Text(String(localized: "passkeys_empty_message"))
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, AppTheme.Spacing.space24)
    }

    private var showsError: Binding<Bool> {
        Binding(
            get: { viewModel.errorMessage != nil },
            set: { presented in
                if !presented {
                    viewModel.clearError()
                }
            }
        )
    }

    private func deletePasskeys(at offsets: IndexSet) {
        for index in offsets {
            let passkey = viewModel.passkeys[index]
            viewModel.remove(id: passkey.id)
        }
    }
}

struct PasskeyRow: View {
    let passkey: Passkey

    var body: some View {
        HStack(spacing: AppTheme.Spacing.space12) {
            Image(systemName: "key.fill")
                .foregroundColor(.secondary)

            VStack(alignment: .leading, spacing: AppTheme.Spacing.space4) {
                Text(passkey.deviceName)
                    .font(.body)
                Text(String(localized: "passkeys_date_added \(addedDateText)"))
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, AppTheme.Spacing.space4)
    }

    private var addedDateText: String {
        Date(timeIntervalSince1970: TimeInterval(passkey.dateAdded))
            .formatted(date: .abbreviated, time: .omitted)
    }
}

struct PasskeysView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            PasskeysView()
        }
    }
}
