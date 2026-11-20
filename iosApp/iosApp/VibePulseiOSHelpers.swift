import SwiftUI
import SharedLogic

enum AppThemePreference: String, CaseIterable, Identifiable {
    case system, light, dark
    var id: String { rawValue }

    var titleKey: LocalizedStringKey {
        switch self {
        case .system: return "profile_theme_system"
        case .light: return "profile_theme_light"
        case .dark: return "profile_theme_dark"
        }
    }

    var colorScheme: ColorScheme? {
        switch self {
        case .system: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }
}


typealias AppThemeMode = AppThemePreference


extension HomeMessage {
    func localized() -> String {
        switch onEnum(of: self) {
        case .profileLoadError:
            return String(localized: "home_error_profile")
        case .locationPermissionRequired:
            return String(localized: "home_error_location_permission")
        case .gpsDisabled:
            return String(localized: "home_error_gps_disabled")
        case .weatherLoadError:
            return String(localized: "home_error_weather")
        case .activitySaved:
            return String(localized: "home_msg_activity_saved")
        case .activitySaveFailed:
            return String(localized: "home_msg_activity_save_failed")
        case .activityTooShort(let msg):
            return String(format: String(localized: "home_msg_activity_too_short"), msg.minSeconds)
        case .playerControlError:
            return String(localized: "home_error_player_control")
        case .skipTrackError:
            return String(localized: "home_error_skip_track")
        case .seekTrackError:
            return String(localized: "home_error_seek_track")
        case .trackLiked:
            return String(localized: "home_msg_track_liked")
        case .trackUnliked:
            return String(localized: "home_msg_track_unliked")
        case .likeUpdateError:
            return String(localized: "home_error_like_failed")
        case .custom(let msg):
            return msg.text
        }
    }
}

extension StatisticsMessage {
    func localized() -> String {
        switch onEnum(of: self) {
        case .loadError:
            return String(localized: "stats_error_load")
        case .userIdentifyError:
            return String(localized: "stats_error_user")
        case .custom(let msg):
            return msg.text
        }
    }
}

extension ProfileMessage {
    func localized() -> String {
        switch onEnum(of: self) {
        case .profileLoadError:
            return String(localized: "home_error_profile")
        case .logoutError:
            return String(localized: "home_error_player_control")
        case .custom(let msg):
            return msg.text
        }
    }
}


extension CadenceZone {
    var localizedTitle: LocalizedStringKey {
        switch self {
        case .idle: return "cadence_zone_idle"
        case .walking: return "cadence_zone_walking"
        case .easyRun: return "cadence_zone_easy_run"
        case .tempoRun: return "cadence_zone_tempo_run"
        case .sprint: return "cadence_zone_sprint"
        default: return "cadence_zone_idle"
        }
    }
}

extension RunStatus {
    var subtitleKey: LocalizedStringKey {
        switch self {
        case .idle: return "run_status_subtitle_idle"
        case .running: return "run_status_subtitle_running"
        case .paused: return "run_status_subtitle_paused"
        default: return "run_status_subtitle_idle"
        }
    }

    var timerStatusKey: LocalizedStringKey {
        switch self {
        case .idle: return "timer_status_idle"
        case .running: return "timer_status_running"
        case .paused: return "timer_status_paused"
        default: return "timer_status_idle"
        }
    }
}

extension ActivitySortOrder {
    var localizedTitle: LocalizedStringKey {
        switch self {
        case .newestFirst: return "stats_sort_newest"
        case .oldestFirst: return "stats_sort_oldest"
        case .mostSteps: return "stats_sort_most_steps"
        case .highestCadence: return "stats_sort_highest_cadence"
        default: return "stats_sort_newest"
        }
    }
}

func formatSecondsToClock(_ totalSeconds: Int64) -> String {
    let hours = totalSeconds / 3600
    let minutes = (totalSeconds % 3600) / 60
    let seconds = totalSeconds % 60
    if hours > 0 {
        return String(format: "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        return String(format: "%02d:%02d", minutes, seconds)
    }
}

func formatMillisToClock(_ millis: Int64) -> String {
    formatSecondsToClock(max(0, millis / 1000))
}


func formatFullDateTime(instant: KotlinInstant) -> String {
    let date = Date(timeIntervalSince1970: TimeInterval(instant.epochSeconds))
    let formatter = DateFormatter()
    formatter.locale = .current
    formatter.dateStyle = .medium
    formatter.timeStyle = .short
    return formatter.string(from: date)
}
