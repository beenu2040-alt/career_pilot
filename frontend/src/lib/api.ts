export interface HealthResponse {
  status: string;
  service: string;
  timestamp: string;
  version: string;
}

export interface UserMeResponse {
  userId: string;
  email: string | null;
  role: string;
  authenticatedAt: string;
}

export interface ProfileResponse {
  userId: string;
  fullName: string | null;
  headline: string | null;
  location: string | null;
  targetRole: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProfileUpdateRequest {
  fullName: string;
  headline: string;
  location: string;
  targetRole: string;
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export async function fetchBackendHealth(): Promise<HealthResponse> {
  const response = await fetch(`${API_BASE_URL}/health`, {
    method: 'GET',
    headers: {
      'Accept': 'application/json',
    },
  });

  if (!response.ok) {
    throw new Error(`Health check failed with HTTP ${response.status}`);
  }

  return response.json();
}

export async function fetchCurrentUserMe(accessToken: string): Promise<UserMeResponse> {
  const response = await fetch(`${API_BASE_URL}/me`, {
    method: 'GET',
    headers: {
      'Accept': 'application/json',
      'Authorization': `Bearer ${accessToken}`,
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to fetch user info (HTTP ${response.status})`);
  }

  return response.json();
}

export async function fetchUserProfile(accessToken: string): Promise<ProfileResponse | null> {
  const response = await fetch(`${API_BASE_URL}/profile`, {
    method: 'GET',
    headers: {
      'Accept': 'application/json',
      'Authorization': `Bearer ${accessToken}`,
    },
  });

  if (response.status === 404) {
    return null; // Profile not yet created
  }

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to fetch profile (HTTP ${response.status})`);
  }

  return response.json();
}

export async function updateUserProfile(
  accessToken: string,
  data: ProfileUpdateRequest
): Promise<ProfileResponse> {
  const response = await fetch(`${API_BASE_URL}/profile`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      'Authorization': `Bearer ${accessToken}`,
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    if (errorData.fieldErrors) {
      const messages = Object.entries(errorData.fieldErrors)
        .map(([k, v]) => `${k}: ${v}`)
        .join(', ');
      throw new Error(`Validation failed: ${messages}`);
    }
    throw new Error(errorData.message || `Failed to update profile (HTTP ${response.status})`);
  }

  return response.json();
}
