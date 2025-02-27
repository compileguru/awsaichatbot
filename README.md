# Chatbot AI on AWS with Cognito Authentication

This project is a serverless chatbot AI running on AWS Lambda, API Gateway, and Cognito for authentication. It uses OpenAI's GPT models to process chat messages.
In addition to this it also has a rest controller to test OpenAi integration locally. To run it locally set your open AI key in OPENAI_API_KEY environment variable.
---

## **1️⃣ Prerequisites**
Ensure you have the following installed:

- **Java 21**
- **Gradle**
- **AWS CLI** (Configured with necessary permissions)
- **AWS SAM CLI**
- **Open API key to access ChatGpt** 

---

## **2️⃣ Build the Project**

Clone the repository and navigate to the project folder:
```sh
 git clone <repository-url>
 cd chatbot
```

Build the project using Gradle:
```sh
./gradlew clean shadowJar
```
This will generate the JAR file inside `build/libs/`.

---

## **3️⃣ Deploying to AWS**

### **Step 1: Package the JAR File & Upload to S3**
```sh
aws s3 cp build/libs/chatbot-0.0.1-SNAPSHOT-all.jar s3://<your-s3-bucket>/chatbot-0.0.1-SNAPSHOT-all.jar
```

### **Step 2: Deploy with AWS SAM**
Modify the template as per your local changes.
You can use template-apikey.yaml if you want to use api key instead of cognito pools.
```sh
sam deploy --template-file template-cognito.yaml --guided
```
Follow the prompts to configure the deployment:
- Stack Name: `chatbot-openai`
- AWS Region: `<your-region>`
- Accept default values where appropriate

Once deployed, you will see the API Gateway URL in the outputs.
Once the stack is deployed:
Update the Open api key with your api key in SSM parameter store.
Create a user in cognito user pool to generate JWT token to access the api (If you are using cognito user pool). 
---

## **4️⃣ Accessing the API**

### **Step 1: Authenticate with Cognito**
To access the API, authenticate using AWS Cognito and obtain a JWT token (Create the user via aws cmd line or UI console prior to this.):
```sh
aws cognito-idp initiate-auth \
    --auth-flow USER_PASSWORD_AUTH \
    --client-id "<COGNITO_APP_CLIENT_ID>" \
    --auth-parameters USERNAME="<your-username>",PASSWORD="<your-password>" \
    --region "<AWS_REGION>"
```

**Response Example:**
```json
{
    "AuthenticationResult": {
        "IdToken": "eyJraWQiOiJLT0Q3Qm...",
        "AccessToken": "eyJraWQiOiJLT0Q3Q...",
        "RefreshToken": "eyJjdHkiOiJKV1Q...",
        "ExpiresIn": 3600,
        "TokenType": "Bearer"
    }
}
```

### **Step 2: Call the API Gateway Endpoint**
Use the **IdToken** from the response in the request:
```sh
curl -X POST "https://<API_ID>.execute-api.<AWS_REGION>.amazonaws.com/Prod/chat" \
     -H "Authorization: Bearer <IdToken>" \
     -H "Content-Type: application/json" \
     -d '{"message": "Hello, how are you?"}'
```

**Expected Response:**
```json
{
    "response": "Hello! How can I assist you today?"
}
```

---

## **5️⃣ Updating the Deployment**

After making changes to the code, rebuild and deploy:
```sh
./gradlew build
sam build
sam deploy
```

---

## **6️⃣ Cleanup**
To remove all deployed resources:
```sh
aws cloudformation delete-stack --stack-name chatbot-openai
or 
sam delete --stack-name chatbot-openai
```

This will delete the API Gateway, Lambda function, Cognito resources, and S3 uploads.

---

## **7️⃣ Troubleshooting**
- **Lambda Logs:**
  ```sh
  sam logs -n ChatbotFunction --tail
  ```
- **Check API Gateway Logs:**
  ```sh
  aws apigateway get-stage --rest-api-id <API_ID> --stage-name Prod
  ```
- **Check Cognito User Pool:**
  ```sh
  aws cognito-idp list-users --user-pool-id <COGNITO_USER_POOL_ID>
  ```
