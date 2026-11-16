//
//  HomeHeaderView.swift
//  iosApp
//
//  Created by Vitalii Pavlyshyn on 16.11.2026.
//

import SwiftUI
import SharedLogic

struct HomeHeaderView: View {
    let user: User?
    let weather: WeatherSnapshot?
    let isWeatherLoading: Bool
    let hasLocationPermission: Bool
    let isLocationEnabled: Bool
    let onRefreshWeather: () -> Void

    @Environment(\.appTheme) private var colors

    var body: some View {
        HStack(spacing: 10) {
            ZStack {
                Circle()
                    .fill(colors.buttonSecondary)
                    .overlay(
                        Circle().stroke(colors.strokePrimary.opacity(0.5), lineWidth: 1)
                    )

                Text(userInitial)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(colors.textPrimary)

                if let avatarString = user?.avatarUrl, let url = URL(string: avatarString) {
                    AsyncImage(url: url) { phase in
                        if let image = phase.image {
                            image
                                .resizable()
                                .scaledToFill()
                        }
                    }
                    .clipShape(Circle())
                }
            }
            .frame(width: 40, height: 40)

            VStack(alignment: .leading, spacing: 2) {
                Text(user?.displayName ?? String(localized: "unknown"))
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(colors.textPrimary)
                    .lineLimit(1)

                Text(user?.hasPremium == true ? "spotify_premium" : "spotify_not_connected")
                    .font(.system(size: 11))
                    .foregroundStyle(colors.textSecondary)
            }

            Spacer(minLength: 8)

            Button(action: onRefreshWeather) {
                HStack(spacing: 6) {
                    if isWeatherLoading {
                        ProgressView()
                            .tint(colors.indicator)
                            .scaleEffect(0.75)
                        Text("weather")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(colors.textSecondary)
                    } else if let weather {
                        if let iconStr = weather.iconUrl, let iconUrl = URL(string: iconStr) {
                            AsyncImage(url: iconUrl) { phase in
                                if let image = phase.image {
                                    image
                                        .resizable()
                                        .renderingMode(.template)
                                        .scaledToFit()
                                        .foregroundStyle(colors.indicator)
                                } else {
                                    Image(systemName: "cloud.sun.fill")
                                        .foregroundStyle(colors.indicator)
                                }
                            }
                            .frame(width: 24, height: 24)
                        } else {
                            Image(systemName: "location.fill")
                                .font(.system(size: 13))
                                .foregroundStyle(colors.indicator)
                        }

                        VStack(alignment: .leading, spacing: 1) {
                            Text("\(Int(weather.temperature.rounded()))°C · \(weather.condition)")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundStyle(colors.textPrimary)

                            if !weather.area.isEmpty {
                                Text(weather.area)
                                    .font(.system(size: 10))
                                    .foregroundStyle(colors.textSecondary)
                            }
                        }
                    } else if !hasLocationPermission {
                        Image(systemName: "location.slash.fill")
                            .font(.system(size: 13))
                            .foregroundStyle(colors.iconSecondary)
                        Text("allow_location")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(colors.textPrimary)
                    } else if !isLocationEnabled {
                        Image(systemName: "location.slash.fill")
                            .font(.system(size: 13))
                            .foregroundStyle(colors.textError)
                        Text("turn_on_gps")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(colors.textError)
                    } else {
                        Image(systemName: "location.fill")
                            .font(.system(size: 13))
                            .foregroundStyle(colors.iconSecondary)
                        Text("update_weather")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(colors.textPrimary)
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(colors.buttonPrimary, in: Capsule())
                .overlay(
                    Capsule().stroke(colors.strokePrimary.opacity(0.5), lineWidth: 1)
                )
            }
            .buttonStyle(.plain)
        }
    }

    private var userInitial: String {
        guard let first = user?.displayName.first else { return "V" }
        return String(first).uppercased()
    }
}
