### How to Run the Server

#### 1-1
```$ npm install``` automatically installs the packages.
Run ```$ pip install -r requirements.txt``` in the `ai` folder to install the required libraries.

#### 1-2
Please copy the contents of the `.env.example` file to create a `.env` file in the `sumdays-backend` folder. You need to change the value of the `Openai api key` to use it.

#### 1-3
Run the central Node.js server with ```$ node app.js``` (in the `sumdays-backend` folder, port 3000).
Run the Python AI server with ```$ python app.py``` (in the `sumdays-backend/ai` folder, port 5001).

#### 1-4
`lang version` cannot be 1.0.0; it must be a lower version.

#### 2-1
Currently, the login feature is implemented separately using Node.js and EC2, and `ec2-15-164-103-159.ap-northeast-2.compute.proxmox.com` is the DNS address. A key file is required to start the server, which we can provide upon request. However, all main features are currently usable without the login function.

---

### How to Use the App

When you first launch the app, a login screen appears. You can either log in or use the "skip login" feature. After accessing the app, a calendar is displayed where you can check diaries written for each date. By pressing the pencil button at the bottom center, you can write memos about your day.

After you finish writing the content for the day, pressing the 'sum' button will take you to a screen to combine the memos. Once the memos are combined well, pressing the proceed button (넘어가기 버튼) in the upper right corner saves the diary for that day. You can still edit the diary afterward.

---

### Features Implemented in Iteration 2

Features implemented in Iteration 2:

1.  **write memo**: You can input the day's events as memos.
2.  **change, delete memo**: You can edit, delete, or change the order of memos.
3.  **drag and drop to sum**: On the 'sum' screen, you can combine memos using drag and drop and receive an AI-generated result from the combination.
4.  **ai backend**: Implemented a backend API that provides AI-generated results when memos are combined.
5.  **read diary**: You can read diaries for each date on the calendar, and if you are not satisfied with the generated result, you can edit it directly.
6.  **undo sum**: You can undo a 'sum' operation in case it was done by mistake.
7.  **others**: In addition, other features were implemented, such as building a DB and providing an API for AI analysis.
