### How to Run the Server

#### 1-1
```$ npm install``` in the `sumdays-backend` folder automatically installs the packages.
Run ```$ pip install -r requirements.txt``` in the `sumdays-backend\ai` folder to install the required libraries.

#### 1-2
Please copy the contents of the `.env.example` file to create a `.env` file in the `sumdays-backend` folder. You need to change the value of the `Openai api key` and `GOOGLE_APPLICATION_CREDENTIALS` to use it.

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
  
After you finish writing the content for the day, pressing the 'sum' button(여우 버튼) will take you to a screen to combine the memos. Once the memos are combined well, pressing the proceed button (넘어가기 버튼) in the upper right corner saves the diary for that day. You can still edit the diary afterward. However, you can also convert voice inputs into memos by pressing the buttons beside the input area.
  
By pressing the bottom right button(프로필 모양 버튼), you can change settings.
- notification settings:
    - You can set a time to receive notifications. When a notification arrives, you can enter a note directly in the notification window.

- style settings:
    - You can choose or create a diary creation style. One basic style is provided, and users can extract styles by inputting text or scanned images in the desired style.

- account settings:
    - You can change your nickname or password.

---

### Features Implemented in Iteration 4

#### Features implemented in Iteration 4:
1. **Diary Generation Streaming**: Implemented real-time streaming output during diary generation.
2. **UI Improvements Based on HE Feedback _(In Progress)_**: Enhancing the overall UI by reflecting Human Evaluation feedback.
3. **Design Enhancements _(In Progress)_**: Updated icons, added loading animations, redesigned memo UI, and implemented voice input animations.
4. **Style Extraction & Style-Based Diary Generation**: Implemented user style extraction and diary generation that applies the extracted style.
5. **Local DB Implementation & Integration**: Built and connected a Room-based local database.
6. **Memo Input Notification Feature**: Added notifications reminding users where user can write memo directly.
7. **Major Revision of Statistics Page _(In Progress)_**: Revamping the statistics page and data flow.
8. **Other Improvements**: Included several minor feature enhancements and overall stability improvements.

#### Next Iteration Plan
- Enhance the completeness of the style extraction and application features and the freedom of customization.
- Improve UI/design completion by incorporating HE feedback.
- Complete the statistics page.
- Server deployment.
- Code refactoring and design pattern application.
- etc.
