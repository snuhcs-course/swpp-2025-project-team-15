### How to Run the Server

#### 1-1
```$ npm install``` automatically installs the packages.
Run ```$ pip install -r requirements.txt``` in the `ai` folder to install the required libraries.

#### 1-2
Please copy the contents of the `.env.example` file to create a `.env` file in the `sumdays-backend` folder. You need to change the value of the `Openai api key` to use it.

#### 1-3
Run the central Node.js server with ```$ node server.js``` (in the `sumdays-backend` folder, port 3000).
Run the Python AI server with ```$ python app.py``` (in the `sumdays-backend/ai` folder, port 5001).

#### 1-4
`lang version` cannot be 1.0.0; it must be a lower version.

#### 2-1
Currently, the login feature is implemented separately using Node.js and EC2, and `ec2-15-164-103-159.ap-northeast-2.compute.proxmox.com` is the DNS address. A key file is required to start the server, which we can provide upon request. However, all main features are currently usable without the login function.

---

### How to Use the App

When you first launch the app, a login screen appears. You can either log in or use the "skip login" feature. After accessing the app, a calendar is displayed where you can check diaries written for each date. By pressing the pencil button at the bottom center, you can write memos about your day.

After you finish writing the content for the day, pressing the 'sum' button will take you to a screen to combine the memos. Once the memos are combined well, pressing the proceed button (넘어가기 버튼) in the upper right corner saves the diary for that day. You can still edit the diary afterward. However, you can also convert images or voices inputs into memos by pressing the buttons beside the input area.

---

### Features Implemented in Iteration 3

Features implemented in Iteration 3:
1. **skip feature**: You can skip the memo-merge process.
2. **diary feedback**: You can receive feedback on your completed diary. You can get emotion scoring, diary summary, and advice from AI.
3. **image to memo**: You can convert photos into memos.
4. **voice to memo**: You can use voice input to create memos.
5. **statistics**: You can view statistics about your written diaries. Weekly/monthly summaries are provided.
6. **others**: UI, DB, etc.
