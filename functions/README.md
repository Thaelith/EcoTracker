# EcoScanner Gemini Proxy

Firebase Cloud Functions that proxy requests to the Gemini API, keeping the API key secure on the server.

## Why This Exists

The Gemini API key was previously embedded in the Android app via `BuildConfig`, making it vulnerable to extraction via reverse engineering. This proxy:

- **Hides the API key** on the server side
- **Enforces authentication** — only logged-in Firebase users can call the proxy
- **Rate limits** requests per user (30/minute)
- **Validates inputs** before sending to Gemini
- **Standardizes responses** so the app gets consistent JSON

## Setup

### 1. Install Firebase CLI

```bash
npm install -g firebase-tools
firebase login
```

### 2. Install Dependencies

```bash
cd functions
npm install
```

### 3. Configure the Gemini API Key

```bash
firebase functions:config:set gemini.key="YOUR_GEMINI_API_KEY"
```

### 4. Deploy

```bash
firebase deploy --only functions
```

After deployment, note the function URLs (or use the callable reference in the app).

## Functions

### `estimateCarbonFootprint` (Callable)

**Request:**
```json
{
  "productTitle": "Organic Almond Milk 1L",
  "category": "Plant-based beverages",
  "quantity": "1L"
}
```

**Response:**
```json
{
  "estimatedCategory": "Plant-based beverages",
  "kgCo2e": 0.45,
  "reasoning": "Almond milk has lower emissions than dairy...",
  "confidence": "High",
  "dataQuality": "Carbon Expert Estimate"
}
```

### `identifyProduct` (Callable)

**Request:**
```json
{
  "barcode": "1234567890123",
  "userHint": "It's a red can of tomato soup, about 400g"
}
```

**Response:**
```json
{
  "productName": "Heinz Cream of Tomato Soup 400g",
  "estimatedCategory": "Canned soups",
  "kgCo2e": 1.2,
  "reasoning": "Tomato soup production involves...",
  "confidence": "Medium",
  "dataQuality": "User-Assisted Estimate"
}
```

## Security

- **Authentication required**: All calls must include a valid Firebase Auth token
- **Rate limiting**: 30 requests per user per minute
- **Input validation**: All fields are validated before reaching Gemini
- **No API key exposure**: The key is stored in Firebase Functions config, never sent to clients

## Local Development

```bash
npm run serve
```

This starts the Firebase emulator suite. The app can be configured to use `http://localhost:5001` during development.
