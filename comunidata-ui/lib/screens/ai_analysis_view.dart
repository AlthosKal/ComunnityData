import 'package:flutter/material.dart';

import '../widgets/common/background_widget.dart';
import '../widgets/common/header_widget.dart';
import '../widgets/components/chat/ai_chat_widget.dart';
import '../widgets/components/csv_management_widget.dart';

class AIAnalysisView extends StatefulWidget {
  const AIAnalysisView({super.key});

  @override
  State<AIAnalysisView> createState() => _AIAnalysisViewState();
}

class _AIAnalysisViewState extends State<AIAnalysisView> with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return BackgroundWidget(
      child: Scaffold(
        backgroundColor: Colors.transparent,
        body: SafeArea(
          child: Column(
            children: [
              _buildHeaderWithTabs(),
              Expanded(
                child: TabBarView(
                  controller: _tabController,
                  children: const [
                    AIChatWidget(),
                    CsvManagementWidget(),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeaderWithTabs() {
    return Container(
      decoration: const BoxDecoration(
        color: Color(0xFF2D3A4F),
      ),
      child: Column(
        children: [
          const HeaderWidget(),
          TabBar(
              controller: _tabController,
              labelColor: Colors.white,
              unselectedLabelColor: const Color(0xFF8FA3B8),
              indicatorColor: const Color(0xFF4A90E2),
              indicatorWeight: 2,
              dividerColor: Colors.transparent,
              labelPadding: EdgeInsets.zero,
              padding: EdgeInsets.zero,
              indicatorPadding: EdgeInsets.zero,
              labelStyle: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w600,
              ),
              unselectedLabelStyle: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.normal,
              ),
              tabs: const [
                Tab(
                  height: 50,
                  icon: Icon(Icons.chat_bubble_outline, size: 18),
                  iconMargin: EdgeInsets.only(bottom: 4),
                  text: 'Chat',
                ),
                Tab(
                  height: 50,
                  icon: Icon(Icons.table_chart, size: 18),
                  iconMargin: EdgeInsets.only(bottom: 4),
                  text: 'CSV',
                ),
              ],
            ),
        ],
      ),
    );
  }
}
