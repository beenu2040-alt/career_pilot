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

export interface SkillDto {
  id: string;
  name: string;
  category: string | null;
  isRequired: boolean;
}

export interface UserSkillDto {
  skillId: string;
  skillName: string;
  category: string | null;
  proficiencyLevel: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'EXPERT';
  yearsExperience: number;
}

export interface UserSkillCreateRequest {
  skillName: string;
  category?: string;
  proficiencyLevel: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'EXPERT';
  yearsExperience?: number;
}

export interface JobMatchResponse {
  jobId: string;
  hasRequiredSkills: boolean;
  matchPercentage: number | null;
  matchedRequiredCount: number;
  totalRequiredCount: number;
  matchedRequiredSkills: string[];
  missingRequiredSkills: string[];
  matchedOptionalCount: number;
  totalOptionalCount: number;
  matchedOptionalSkills: string[];
  explanation: string;
}

export interface JobSummaryResponse {
  id: string;
  title: string;
  company: string;
  location: string;
  employmentType: string;
  remoteType: string | null;
  experienceLevel: string | null;
  salaryMin: number | null;
  salaryMax: number | null;
  salaryCurrency: string;
  postedAt: string;
  isSample: boolean;
  requiredSkills: SkillDto[];
  optionalSkills: SkillDto[];
  isSaved: boolean;
  matchPercentage: number | null;
}

export interface JobDetailResponse extends JobSummaryResponse {
  description: string;
  sourceUrl: string | null;
  createdAt: string;
  updatedAt: string;
  match: JobMatchResponse | null;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  isFirst: boolean;
  isLast: boolean;
}

export interface JobFilterParams {
  search?: string;
  location?: string;
  employmentType?: string;
  remoteType?: string;
  experienceLevel?: string;
  skill?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface SaveJobResponse {
  jobId: string;
  isSaved: boolean;
  message: string;
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
    return null;
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

// ----------------------------------------------------------------------------
// Phase 3: Job Discovery & Matching API Client
// ----------------------------------------------------------------------------

export async function fetchJobs(
  params: JobFilterParams = {},
  accessToken?: string | null
): Promise<PageResponse<JobSummaryResponse>> {
  const query = new URLSearchParams();
  if (params.search) query.set('search', params.search);
  if (params.location) query.set('location', params.location);
  if (params.employmentType) query.set('employmentType', params.employmentType);
  if (params.remoteType) query.set('remoteType', params.remoteType);
  if (params.experienceLevel) query.set('experienceLevel', params.experienceLevel);
  if (params.skill) query.set('skill', params.skill);
  if (params.page !== undefined) query.set('page', params.page.toString());
  if (params.size !== undefined) query.set('size', params.size.toString());
  if (params.sort) query.set('sort', params.sort);

  const headers: Record<string, string> = {
    'Accept': 'application/json',
  };
  if (accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`;
  }

  const response = await fetch(`${API_BASE_URL}/jobs?${query.toString()}`, {
    method: 'GET',
    headers,
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to fetch jobs (HTTP ${response.status})`);
  }

  return response.json();
}

export async function fetchJobDetail(
  id: string,
  accessToken?: string | null
): Promise<JobDetailResponse> {
  const headers: Record<string, string> = {
    'Accept': 'application/json',
  };
  if (accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`;
  }

  const response = await fetch(`${API_BASE_URL}/jobs/${id}`, {
    method: 'GET',
    headers,
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to fetch job detail (HTTP ${response.status})`);
  }

  return response.json();
}

export async function fetchSavedJobs(
  accessToken: string,
  page: number = 0,
  size: number = 10
): Promise<PageResponse<JobSummaryResponse>> {
  const response = await fetch(`${API_BASE_URL}/jobs/saved?page=${page}&size=${size}`, {
    method: 'GET',
    headers: {
      'Accept': 'application/json',
      'Authorization': `Bearer ${accessToken}`,
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to fetch saved jobs (HTTP ${response.status})`);
  }

  return response.json();
}

export async function saveJob(
  id: string,
  accessToken: string
): Promise<SaveJobResponse> {
  const response = await fetch(`${API_BASE_URL}/jobs/${id}/save`, {
    method: 'POST',
    headers: {
      'Accept': 'application/json',
      'Authorization': `Bearer ${accessToken}`,
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to bookmark job (HTTP ${response.status})`);
  }

  return response.json();
}

export async function unsaveJob(
  id: string,
  accessToken: string
): Promise<SaveJobResponse> {
  const response = await fetch(`${API_BASE_URL}/jobs/${id}/save`, {
    method: 'DELETE',
    headers: {
      'Accept': 'application/json',
      'Authorization': `Bearer ${accessToken}`,
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to remove bookmark (HTTP ${response.status})`);
  }

  return response.json();
}

export async function fetchJobRecommendations(
  accessToken: string,
  page: number = 0,
  size: number = 10
): Promise<PageResponse<JobSummaryResponse>> {
  const response = await fetch(`${API_BASE_URL}/jobs/recommendations?page=${page}&size=${size}`, {
    method: 'GET',
    headers: {
      'Accept': 'application/json',
      'Authorization': `Bearer ${accessToken}`,
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to fetch recommendations (HTTP ${response.status})`);
  }

  return response.json();
}

export async function fetchJobMatch(
  id: string,
  accessToken: string
): Promise<JobMatchResponse> {
  const response = await fetch(`${API_BASE_URL}/jobs/${id}/match`, {
    method: 'GET',
    headers: {
      'Accept': 'application/json',
      'Authorization': `Bearer ${accessToken}`,
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to fetch match score (HTTP ${response.status})`);
  }

  return response.json();
}

// ----------------------------------------------------------------------------
// Phase 3: Skills API Client
// ----------------------------------------------------------------------------

export async function fetchCatalogSkills(): Promise<SkillDto[]> {
  const response = await fetch(`${API_BASE_URL}/skills`, {
    method: 'GET',
    headers: {
      'Accept': 'application/json',
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to load skills catalog (HTTP ${response.status})`);
  }

  return response.json();
}

export async function fetchUserSkills(accessToken: string): Promise<UserSkillDto[]> {
  const response = await fetch(`${API_BASE_URL}/profile/skills`, {
    method: 'GET',
    headers: {
      'Accept': 'application/json',
      'Authorization': `Bearer ${accessToken}`,
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to load user skills (HTTP ${response.status})`);
  }

  return response.json();
}

export async function addUserSkill(
  data: UserSkillCreateRequest,
  accessToken: string
): Promise<UserSkillDto> {
  const response = await fetch(`${API_BASE_URL}/profile/skills`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      'Authorization': `Bearer ${accessToken}`,
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to add skill (HTTP ${response.status})`);
  }

  return response.json();
}

export async function removeUserSkill(
  skillId: string,
  accessToken: string
): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/profile/skills/${skillId}`, {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to remove skill (HTTP ${response.status})`);
  }
}

// ============================================================================
// Phase 4: Resume Analyzer & LLM Feedback Types & APIs
// ============================================================================

export interface ResumeSummary {
  id: string;
  originalFilename: string;
  fileSize: number;
  mimeType: string;
  processingStatus: 'UPLOADED' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  createdAt: string;
  updatedAt: string;
  latestScore: number | null;
  latestAnalysisId: string | null;
}

export interface DimensionScore {
  dimension: string;
  score: number;
  maxScore: number;
  weightPercentage: number;
  assessmentType: 'DETERMINISTIC' | 'MODEL_ASSESSED' | 'HYBRID';
  feedback: string;
}

export interface ExtractedData {
  candidateName: string;
  email: string;
  phone: string;
  location: string;
  detectedSections: string[];
  wordCount: number;
  quantifiableMetricsCount: number;
  education: string[];
  roles: string[];
}

export interface StrengthItem {
  title: string;
  description: string;
  evidence: string;
}

export interface ImprovementItem {
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  category: string;
  title: string;
  issue: string;
  suggestion: string;
  exampleBefore: string;
  exampleAfter: string;
}

export interface SkillEvidenceItem {
  name: string;
  category: string;
  hasContext: boolean;
  evidence: string;
}

export interface SkillAnalysis {
  technicalSkills: SkillEvidenceItem[];
  softSkills: SkillEvidenceItem[];
  skillsWithoutEvidence: string[];
}

export interface ExperienceAnalysis {
  totalRolesIdentified: number;
  actionVerbRating: 'STRONG' | 'MODERATE' | 'NEEDS_IMPROVEMENT';
  quantifiableMetricRating: string;
  roleSummaries: string[];
}

export interface JobMatchAnalysis {
  jobId: string;
  jobTitle: string;
  company: string;
  matchedSkills: string[];
  missingRequiredSkills: string[];
  missingOptionalSkills: string[];
  matchScore: number;
  tailoredRecommendations: string[];
}

export interface ResumeAnalysis {
  id: string;
  resumeId: string;
  jobId: string | null;
  analysisStatus: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  overallScore: number;
  summary: string;
  dimensionScores: DimensionScore[];
  extractedData: ExtractedData;
  strengths: StrengthItem[];
  improvements: ImprovementItem[];
  skillAnalysis: SkillAnalysis;
  experienceAnalysis: ExperienceAnalysis;
  jobMatch: JobMatchAnalysis | null;
  modelIdentifier: string;
  createdAt: string;
  completedAt: string;
}

export async function uploadResume(
  file: File,
  accessToken: string
): Promise<ResumeSummary> {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(`${API_BASE_URL}/resumes`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Accept': 'application/json',
    },
    body: formData,
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to upload resume (HTTP ${response.status})`);
  }

  return response.json();
}

export async function fetchUserResumes(
  accessToken: string
): Promise<ResumeSummary[]> {
  const response = await fetch(`${API_BASE_URL}/resumes`, {
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Accept': 'application/json',
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to fetch resumes (HTTP ${response.status})`);
  }

  return response.json();
}

export async function fetchResumeById(
  resumeId: string,
  accessToken: string
): Promise<ResumeSummary> {
  const response = await fetch(`${API_BASE_URL}/resumes/${resumeId}`, {
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Accept': 'application/json',
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to fetch resume (HTTP ${response.status})`);
  }

  return response.json();
}

export async function deleteResume(
  resumeId: string,
  accessToken: string
): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/resumes/${resumeId}`, {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to delete resume (HTTP ${response.status})`);
  }
}

export async function triggerResumeAnalysis(
  resumeId: string,
  accessToken: string,
  jobId?: string
): Promise<ResumeAnalysis> {
  const response = await fetch(`${API_BASE_URL}/resumes/${resumeId}/analyze`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      'Authorization': `Bearer ${accessToken}`,
    },
    body: JSON.stringify(jobId ? { jobId } : {}),
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Analysis failed (HTTP ${response.status})`);
  }

  return response.json();
}

export async function fetchResumeAnalyses(
  resumeId: string,
  accessToken: string
): Promise<ResumeAnalysis[]> {
  const response = await fetch(`${API_BASE_URL}/resumes/${resumeId}/analyses`, {
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Accept': 'application/json',
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to fetch analyses (HTTP ${response.status})`);
  }

  return response.json();
}

export async function fetchResumeAnalysisById(
  resumeId: string,
  analysisId: string,
  accessToken: string
): Promise<ResumeAnalysis> {
  const response = await fetch(`${API_BASE_URL}/resumes/${resumeId}/analyses/${analysisId}`, {
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Accept': 'application/json',
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to fetch analysis (HTTP ${response.status})`);
  }

  return response.json();
}

// ============================================================================
// Phase 5: Career Roadmap Interfaces and Client Functions
// ============================================================================

export interface SkillGap {
  skill: string;
  category: string;
  status: 'ACQUIRED' | 'GAP' | 'NEEDS_EVIDENCE';
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  marketDemandFrequency: number;
  evidenceOrContext: string;
}

export interface SkillGapSummary {
  totalTargetSkills: number;
  acquiredSkillsCount: number;
  gapSkillsCount: number;
  readinessScore: number;
  acquiredSkills: SkillGap[];
  gapSkills: SkillGap[];
  keyStrengths: string[];
  priorityGaps: string[];
}

export interface MilestoneSkill {
  name: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  rationale: string;
}

export interface MilestoneProject {
  title: string;
  description: string;
  deliverable: string;
  techStack: string[];
}

export interface MilestoneResource {
  title: string;
  type: string;
  url: string;
}

export interface RoadmapMilestone {
  id: string;
  roadmapId: string;
  title: string;
  description: string;
  sequenceOrder: number;
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED';
  estimatedDurationWeeks: number;
  completedAt: string | null;
  skills: MilestoneSkill[];
  projects: MilestoneProject[];
  resources: MilestoneResource[];
  completionCriteria: string[];
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface RoadmapSummary {
  id: string;
  targetRole: string;
  currentLevel: string;
  overallProgress: number;
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED';
  summary: string;
  milestonesCount: number;
  completedMilestonesCount: number;
  totalEstimatedWeeks: number;
  createdAt: string;
  updatedAt: string;
}

export interface RoadmapDetail {
  id: string;
  userId: string;
  targetRole: string;
  currentLevel: string;
  overallProgress: number;
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED';
  summary: string;
  skillGapSummary: SkillGapSummary | null;
  milestones: RoadmapMilestone[];
  modelIdentifier: string;
  createdAt: string;
  updatedAt: string;
}

export interface GenerateRoadmapPayload {
  targetRole?: string;
  currentLevel?: string;
}

export interface UpdateMilestonePayload {
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED';
  notes?: string;
}

export async function generateCareerRoadmap(
  payload: GenerateRoadmapPayload,
  accessToken: string
): Promise<RoadmapDetail> {
  const response = await fetch(`${API_BASE_URL}/roadmaps/generate`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      'Authorization': `Bearer ${accessToken}`,
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Roadmap generation failed (HTTP ${response.status})`);
  }

  return response.json();
}

export async function fetchCareerRoadmaps(accessToken: string): Promise<RoadmapSummary[]> {
  const response = await fetch(`${API_BASE_URL}/roadmaps`, {
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Accept': 'application/json',
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to fetch roadmaps (HTTP ${response.status})`);
  }

  return response.json();
}

export async function fetchLatestCareerRoadmap(accessToken: string): Promise<RoadmapDetail | null> {
  const response = await fetch(`${API_BASE_URL}/roadmaps/latest`, {
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Accept': 'application/json',
    },
  });

  if (response.status === 204 || response.status === 404) {
    return null;
  }

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to fetch latest roadmap (HTTP ${response.status})`);
  }

  return response.json();
}

export async function fetchCareerRoadmapById(id: string, accessToken: string): Promise<RoadmapDetail> {
  const response = await fetch(`${API_BASE_URL}/roadmaps/${id}`, {
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Accept': 'application/json',
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to fetch roadmap (HTTP ${response.status})`);
  }

  return response.json();
}

export async function updateMilestoneStatus(
  roadmapId: string,
  milestoneId: string,
  payload: UpdateMilestonePayload,
  accessToken: string
): Promise<RoadmapDetail> {
  const response = await fetch(`${API_BASE_URL}/roadmaps/${roadmapId}/milestones/${milestoneId}`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      'Authorization': `Bearer ${accessToken}`,
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to update milestone (HTTP ${response.status})`);
  }

  return response.json();
}

export async function deleteCareerRoadmap(roadmapId: string, accessToken: string): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/roadmaps/${roadmapId}`, {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
    },
  });

  if (!response.ok && response.status !== 204) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to delete roadmap (HTTP ${response.status})`);
  }
}

